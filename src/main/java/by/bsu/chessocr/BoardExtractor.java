package by.bsu.chessocr;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.circle;
import static org.bytedeco.javacpp.opencv_core.line;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_TC89_KCOS;
import static org.bytedeco.javacpp.opencv_imgproc.RETR_CCOMP;
import static org.bytedeco.javacpp.opencv_imgproc.approxPolyDP;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.convexHull;
import static org.bytedeco.javacpp.opencv_imgproc.drawContours;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.getPerspectiveTransform;
import static org.bytedeco.javacpp.opencv_imgproc.warpPerspective;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;

/**
 * Класс содержит логику извлечения из захваченного с камеры изображения изображения доски
 *
 */
public class BoardExtractor {

	private Mat m;
	private Size size;

	public BoardExtractor() {

	}

	public boolean isInitialized() {
		return m != null && size != null;
	}

	// тестовая отрисовка найденного контура
	public void testDarawContour(Mat img) {
		MatVector contours = extractContours(img);
		int contourIdx = findBestContourIndex(contours, img);

		drawContours(img, contours, contourIdx, COLOR1, 2, 8, new Mat(), Integer.MAX_VALUE, new Point(0, 0));
	}

	// инициализация 
	public void init(Mat img) {
		// копия исходного изображения для отладки
		Mat img_debug = img.clone();

		// поиск контуров на исходном изображении
		MatVector contours = extractContours(img);
		// поиск лучшего контура, возвращается его порядковый номера
		int contourIdx = findBestContourIndex(contours, img);

		// отрисовка выбранного контура контура на исходном изодбражении для отладки 
		drawContours(img_debug, contours, contourIdx, COLOR1, 2, 8, new Mat(), Integer.MAX_VALUE, new Point(0, 0));

		// сохраянем ссылку на контур
		Mat contour = contours.get(contourIdx);

		// апроксимация замкнутой кривой (контура), чтобы получить вершины 4-хугольника
		Mat approxCurve = new Mat();
		approxPolyDP(contour, approxCurve, 10, true);

		// проверка на похожесть найденного многоугольника на четырехугольник
		if (approxCurve.total() < 4 || approxCurve.total() > 4) {
			System.err.println("Quad not found");
			return;
		}

		// отладочная отрисовка
		if (approxCurve.total() == 4) {
			List<Point> listOfPointsCurve = contourToListOfPoints(approxCurve);
			testDrawContourByPoins(listOfPointsCurve, img_debug);
		}

		// предполагаемые реальные размеры доски, возвращается прямоугольник, охватывающий контур
		Rect boundingRect = boundingRect(contour);

		// т.к. доска обычно квадратная, то используем только ширину
		int width = boundingRect.width();

		// определяем размеры результирующей картинки, исходя из известной ширины
		Mat outputQuad = quad(0, width, //
				width, width, //
				width, 0, //
				0, 0);

		// матрица координат исходного четырехугольника
		Mat inputQuad = new Mat();

		// отсортируем точки в порядке, которому соответсвуют outputQuad
		convexHull(approxCurve, inputQuad, true, true);

		// конфертируем в матрицу нужной размерности
		inputQuad.convertTo(inputQuad, CV_32F);

		// матрица для всех последующих преобразований
		m = getPerspectiveTransform(inputQuad, outputQuad);

		// сохраняем размеры изображения для последующего преображония
		size = new Size(2);
		size.asBuffer().put(width).put(width);
	}

	// функция извлечения изображения доски 
	public Mat extract(Mat img) {
		// матрица для хранения результирующего изображения
		Mat dest = new Mat(size, img.type());
		// преобразуем перспективу, используя сохраненные ранее m и size
		warpPerspective(img, dest, m, size);
		return dest;
	}

	// функция создания матрицы, содержащей координаты четырехугольника
	private Mat quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		Mat quad = new Mat(4, 2, CV_32F);
		FloatIndexer indxr = quad.createIndexer();
		indxr.put(0, 0, x1);
		indxr.put(0, 1, y1);

		indxr.put(1, 0, x2);
		indxr.put(1, 1, y2);

		indxr.put(2, 0, x3);
		indxr.put(2, 1, y3);

		indxr.put(3, 0, x4);
		indxr.put(3, 1, y4);
		return quad;
	}

	// --------------------------------------------------

	private Scalar COLOR1 = new Scalar(0, 0, 255, 0);

	private Scalar COLOR3 = new Scalar(0, 100, 100, 100);

	// функция поиска контуров
	private MatVector extractContours(Mat src) {
		// конвертируем в черно-белое изображение
		Mat img_bw = Utils.toBW(src);
		// переменные для хранения результата поиска
		MatVector contours = new MatVector();
		Mat hierarchy = new Mat();
		/*
		 * CV_RETR_CCOMP retrieves all of the contours and organizes them into a
		 * two-level hierarchy. At the top level, there are external boundaries
		 * of the components. At the second level, there are boundaries of the
		 * holes. If there is another contour inside a hole of a connected
		 * component, it is still put at the top level.
		 * 
		 * CV_CHAIN_APPROX_TC89_KCOS applies one of the flavors of the Teh-Chin
		 * chain approximation algorithm
		 */
		findContours(img_bw, contours, hierarchy, RETR_CCOMP, CV_CHAIN_APPROX_TC89_KCOS);
		return contours;
	}

	// функция поиска наиболее подходящего контура
	private int findBestContourIndex(MatVector contours, Mat srcImage) {
		// веременная переменная для хранения контура
		Mat contour = null;
		// переменная для индекса найденного конутра
		int contourIdx = -1;
		// общая площадь изображения
		double imgArea = srcImage.arrayWidth() * srcImage.arrayHeight();
		// перебор всех контуров
		for (int i = 0; i < contours.size(); i++) {
			contour = contours.get(i);
			contourIdx = i;

			// охватывающий прямоугольник
			Rect boundingRect = boundingRect(contour);
			// соотношение сторон найденного прямогольника
			double rectRatio = Utils.ratio(boundingRect);

			// площадь фигуры, ограниченной контуром
			double contourArea = contourArea(contour);

			// соотношение площади фигуры ограниченной контуром к общая площади рисунка 
			double imgContourRatio = Utils.ratio(imgArea, contourArea);

			// если площадь контура более четверти изображения - это подходящий контур
			if (imgContourRatio > 0.25) {
				System.out.println(
						boundingRect.width() + "x" + boundingRect.height() + " " + rectRatio + " " + imgContourRatio);
				break;
			}
		}
		return contourIdx;
	}

	// функция преобразования контура в список точек 
	private List<Point> contourToListOfPoints(Mat contour) {
		ArrayList<Point> result = new ArrayList<>();
		IntIndexer points = contour.createIndexer(false);
		for (int pointIdx = 0; pointIdx < contour.rows(); pointIdx++) {
			int x = points.get(pointIdx, 0);
			int y = points.get(pointIdx, 1);
			result.add(new Point(x, y));
		}
		return result;
	}

	// отладочная отрисовка списка точек
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

}
