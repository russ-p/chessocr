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

		int ddRow = dRow / 4;
		int ddCol = dCol / 4;

		int x = cellId.getX();
		int y = cellId.getY();
		RangePair rangePair = new RangePair(y * dRow + ddRow, y * dRow + dRow - ddRow, x * dCol + ddCol,
				x * dCol + dCol - ddCol);
		return rangePair.regionCopy(board);
	}

	@SuppressWarnings("unused")
	private void writeTestPiece(Mat region, CellId cellId) {
		Utils.writeTestImage("reg-" + cellId, region);
	}

	public byte check(CellId cellId, Mat img) {
		// регион исходного изображения для сравнения в HSV
		Mat srcRegion = Utils.toHSV(getCell(cellId, srcBoard));

		// средняя яркость текущего изображения
		Scalar scalar = opencv_core.mean(srcRegion);
		// исходная клетка слишком белая - шашек там быть не должно
		if (scalar.get(2) > 150) {
			return 0;
		}

		// регион текущего изображения для сравнения в HSV
		Mat imgRegion = Utils.toHSV(getCell(cellId, img));

		// средняя яркость текущего изображения
		scalar = opencv_core.mean(imgRegion);
		// клетка изображения более белая или цветная
		// Saturation < 50; Value > 150
		byte color = (byte) ((scalar.get(2) > 150 && scalar.get(1) < 50) ? 2 : 1);

		Mat diff = new Mat();
		opencv_core.absdiff(srcRegion, imgRegion, diff);
		opencv_imgproc.threshold(diff, diff, 50, 255, opencv_imgproc.THRESH_BINARY);

		Scalar dstMean = opencv_core.mean(diff);

		double d = dstMean.get(2);
		
		Utils.writeTestImage("reg-" + cellId + "-s", getCell(cellId, srcBoard));
		Utils.writeTestImage("reg-" + cellId + "-i", getCell(cellId, img));
		writeTestPiece(diff, cellId);

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
