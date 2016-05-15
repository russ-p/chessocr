package by.bsu.chessocr;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Range;
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

	@SuppressWarnings("unused")
	private void writeTestPiece(Mat region, CellId cellId) {
		Utils.writeTestImage("reg-" + cellId, region);
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

		// writeTestPiece(diff, cellId );

		Scalar dstMean = opencv_core.mean(diff);

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
