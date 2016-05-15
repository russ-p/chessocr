package by.bsu.chessocr;

import static org.bytedeco.javacpp.opencv_core.circle;
import static org.bytedeco.javacpp.opencv_core.line;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.RETR_CCOMP;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;

import java.util.List;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Range;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_imgproc;

public class CellsExtractor {

	private Mat srcBoard;

	private double dMean = 5;

	public CellsExtractor() {
	}

	public void init(Mat board) {
		this.srcBoard = board.clone();
	}

	public double getdMean() {
		return dMean;
	}

	public void setdMean(double dMean) {
		this.dMean = dMean;
	}

	private Mat getCell(CellId cellId, Mat board) {
		int rows = board.rows();
		int cols = board.cols();

		int dRow = rows / 8;
		int dCol = cols / 8;

		int ddRow = dRow / 10;
		int ddCol = dCol / 10;

		int x = cellId.getX();
		int y = cellId.getY();
		RangePair rangePair = new RangePair(Math.max(0, y * dRow - ddRow), Math.min(y * dRow + dRow + ddRow, rows),
				Math.max(0, x * dCol - ddCol), Math.min(x * dCol + dCol + ddCol, cols));
		return rangePair.regionCopy(board);
	}

	private MatVector extractContours(Mat src) {
		Mat img_bw = Utils.toBW(src);
		MatVector contours = new MatVector();
		Mat hierarchy = new Mat();
		findContours(img_bw, contours, hierarchy, RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE);
		return contours;
	}

	private int findBestContourIndex(MatVector contours, Mat srcImage) {
		Mat contour = null;
		int contourIdx = -1;
		double imgArea = srcImage.arrayWidth() * srcImage.arrayHeight();
		for (int i = 0; i < contours.size(); i++) {
			contour = contours.get(i);
			contourIdx = i;

			Rect boundingRect = boundingRect(contour);
			double rectRatio = Utils.ratio(boundingRect);
			if (rectRatio < 0.9) {
				continue;
			}

			double contourArea = contourArea(contour);
			double imgContourRatio = Utils.ratio(imgArea, contourArea);

			if (imgContourRatio < 0.8) {
				continue;
			}
			break;
		}
		return contourIdx;
	}

	private void writeTestPiece(Mat region, CellId cellId) {
		Utils.writeTestImage("reg-" + cellId, region);
	}

	private Scalar COLOR1 = new Scalar(0, 0, 255, 0);
	private Scalar COLOR3 = new Scalar(0, 128, 255, 0);

	private void testDrawContourByPoins(List<Point> listOfPoints, Mat img) {
		Point first = null;
		Point prev = null;
		int r = 3;
		for (Point point : listOfPoints) {
			circle(img, point, r, COLOR1, -1, 0, 0);
			System.out.println(point.x() + ":" + point.y());
			if (first == null) {
				first = point;
				prev = point;
				continue;
			}
			line(img, prev, point, COLOR1);
			prev = point;
		}
		line(img, prev, first, COLOR3);
	}

	public byte check(CellId cellId, Mat img) {

		Mat hsvSrcCell = Utils.toHSV(getCell(cellId, srcBoard));
		Scalar scalar = opencv_core.mean(hsvSrcCell);
		// исходная клетка слишком белая - шашек там быть не должно
		if (scalar.get(2) > 150) {
			return 0;
		}
		

		Mat hsvImgCell = Utils.toHSV(getCell(cellId, img));
		scalar = opencv_core.mean(hsvImgCell);
		// клетка изображения более белая или черная
		byte color = (byte) ((scalar.get(2) > 150) ? 2 : 1);


		Mat srcRegion = Utils.toHSV(getCell(cellId, srcBoard));
		Mat imgRegion = Utils.toHSV(getCell(cellId, img));

		
		Mat diff = new Mat();
		opencv_core.absdiff(srcRegion, imgRegion, diff);
		opencv_imgproc.threshold(diff, diff, 50, 255, opencv_imgproc.THRESH_BINARY);
		
		//writeTestPiece(diff, cellId );
		
		Scalar dstMean = opencv_core.mean(diff);

/*		Scalar srcMean = opencv_core.mean(srcRegion);
		Scalar imgMean = opencv_core.mean(imgRegion);
		double d = Math.abs(srcMean.get(2) - imgMean.get(2));*/
		
		double d = dstMean.get(2);

		return (byte) (d > dMean ? color : 0);
	}

	private final class RangePair {

		private final Range rowRange, colRange;

		RangePair(int s1, int e1, int s2, int e2) {
			rowRange = new Range(s1, e1);
			colRange = new Range(s2, e2);
		};

		Mat regionCopy(Mat src) {
			return src.apply(rowRange, colRange).clone();
		}

	}

}
