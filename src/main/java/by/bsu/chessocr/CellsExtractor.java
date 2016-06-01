package by.bsu.chessocr;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Range;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_imgproc;

/**
 * Класс отвечает за получение информации о каждой клетке изображения доски
 *
 */
public class CellsExtractor {

	// запомненной исходное изображеине доски
	private Mat srcBoard;

	// пороговое значение изменения изображения, которое определяет наличие шашки на клетке
	private double dMean = 5;

	public CellsExtractor() {
	}

	// установка начального изображения доски (без шашек)
	public void init(Mat board) {
		this.srcBoard = board.clone();
	}

	// получение текущего порогового значения
	public double getdMean() {
		return dMean;
	}

	// установка текущего порогового значения
	public void setdMean(double dMean) {
		this.dMean = dMean;
	}

	// получение изображения одной клетки из изображения целой доски
	private Mat getCell(CellId cellId, Mat board) {
		// размер изображения по вертикали
		int rows = board.rows();
		// разер изображения по горизолнтали
		int cols = board.cols();

		// высота одной клетки
		int dRow = rows / 8;
		// ширина одной клетки
		int dCol = cols / 8;

		// поправка на высоту и ширину клетки
		int ddRow = dRow / 4;
		int ddCol = dCol / 4;

		// начальная координата клетки
		int x = cellId.getX();
		int y = cellId.getY();
		// пара диапазонов координат изображения клетки на изображении всей доски
		RangePair rangePair = new RangePair(y * dRow + ddRow, y * dRow + dRow - ddRow, x * dCol + ddCol,
				x * dCol + dCol - ddCol);
		// копирование участка изображения, возвращается требуемое изображение одной клетки
		return rangePair.regionCopy(board);
	}

	@SuppressWarnings("unused")
	private void writeTestPiece(Mat region, CellId cellId) {
		Utils.writeTestImage("reg-" + cellId, region);
	}

	// анализ одной клетки на изображении
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
		// молучаем матрицу разностей между исходным и текущим изображением
		opencv_core.absdiff(srcRegion, imgRegion, diff);
		opencv_imgproc.threshold(diff, diff, 50, 255, opencv_imgproc.THRESH_BINARY);

		// среднее значение для матрицы разности
		Scalar dstMean = opencv_core.mean(diff);

		// используем значение яркости для сравнение с пороговым
		double d = dstMean.get(2);
		
		// тестовый вывод изображения региона
		//Utils.writeTestImage("reg-" + cellId + "-s", getCell(cellId, srcBoard));
		//Utils.writeTestImage("reg-" + cellId + "-i", getCell(cellId, img));
		//writeTestPiece(diff, cellId);

		// если значение  больше порога, то считаем что клетка изменилась
		return (byte) (d > dMean ? color : 0);
	}

	// свпомогательные класс для извлечения региона из изображения
	private final class RangePair {

		private final Range rowRange, colRange;

		RangePair(int s1, int e1, int s2, int e2) {
			rowRange = new Range(s1, e1);
			colRange = new Range(s2, e2);
		};

		// копирование региона изображения
		Mat regionCopy(Mat src) {
			return src.apply(rowRange, colRange).clone();
		}

	}

}
