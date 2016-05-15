package by.bsu.chessocr;

import static java.lang.Math.PI;
import static java.util.stream.Collectors.toList;
import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.circle;
import static org.bytedeco.javacpp.opencv_core.line;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_TC89_KCOS;
import static org.bytedeco.javacpp.opencv_imgproc.HoughLines;
import static org.bytedeco.javacpp.opencv_imgproc.RETR_CCOMP;
import static org.bytedeco.javacpp.opencv_imgproc.approxPolyDP;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.convexHull;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
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

public class BoardExtractor {

	private Mat m;
	private Size size;

	public BoardExtractor() {

	}

	public boolean isInitialized() {
		return m != null && size != null;
	}

	public void testDarawContour(Mat img) {
		MatVector contours = extractContours(img);
		int contourIdx = findBestContourIndex(contours, img);

		drawContours(img, contours, contourIdx, COLOR1, 2, 8, new Mat(), Integer.MAX_VALUE, new Point(0, 0));
	}
	
	public void init(Mat img) {
		Mat img_debug = img.clone();

		MatVector contours = extractContours(img);
		int contourIdx = findBestContourIndex(contours, img);

		drawContours(img_debug, contours, contourIdx, COLOR1, 2, 8, new Mat(), Integer.MAX_VALUE, new Point(0, 0));

		Mat contour = contours.get(contourIdx);
		List<Point> listOfPoints = contourToListOfPoints(contour);

		// testDrawContourByPoins(listOfPoints, img);

		Point[] corners = findCorners(listOfPoints);

		// testDrawContourByPoins(Arrays.asList(corners), img);

		// отрисовка контура
		/*
		 * Mat img_contour = new Mat(img.size(), img.type());
		 * img_contour.put(BLACK); drawContours(img_contour, contours,
		 * contourIdx, COLOR1, 1, 8, new Mat(), Integer.MAX_VALUE, new Point(0,
		 * 0));
		 * 
		 * perspective(img_contour, img);
		 */

		// апроксимация замкнутой курвы, чтобы получить вершины 4-хугольника
		Mat approxCurve = new Mat();
		approxPolyDP(contour, approxCurve, 10, true);

		if (approxCurve.total() < 4 || approxCurve.total() > 4) {
			System.err.println("Quad not found");
			return;
		}

		if (approxCurve.total() == 4) {
			List<Point> listOfPointsCurve = contourToListOfPoints(approxCurve);
			testDrawContourByPoins(listOfPointsCurve, img_debug);
		}

		// предполагаемые реальные размеры доски
		Rect boundingRect = boundingRect(contour);

		// т.к. доска обычно квадратная то используем только ширину
		int width = boundingRect.width();

		// определяем размеры результирующей картинки
		Mat outputQuad = quad(0, width, //
				width, width, //
				width, 0, //
				0, 0);

		Mat inputQuad = new Mat();

		// отсортируем точки в порядке, которому соответсвуют outputQuad
		convexHull(approxCurve, inputQuad, true, true);

		inputQuad.convertTo(inputQuad, CV_32F);

		// матрица для всех последующих преобразований
		m = getPerspectiveTransform(inputQuad, outputQuad);

		size = new Size(2);
		size.asBuffer().put(width).put(width);
	}

	public Mat extract(Mat img) {
		Mat dest = new Mat(size, img.type());
		warpPerspective(img, dest, m, size);
		return dest;
	}

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

	private Scalar COLOR2 = new Scalar(0, 125, 255, 255);
	private Scalar COLOR3 = new Scalar(0, 100, 100, 100);

	private Scalar BLACK = new Scalar(0, 0, 0, 0);

	private MatVector extractContours(Mat src) {
		Mat img_bw = Utils.toBW(src);
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

	private int findBestContourIndex(MatVector contours, Mat srcImage) {
		Mat contour = null;
		int contourIdx = -1;
		double imgArea = srcImage.arrayWidth() * srcImage.arrayHeight();
		for (int i = 0; i < contours.size(); i++) {
			contour = contours.get(i);
			contourIdx = i;

			Rect boundingRect = boundingRect(contour);
			double rectRatio = Utils.ratio(boundingRect);

			double contourArea = contourArea(contour);

			double imgContourRatio = Utils.ratio(imgArea, contourArea);

			if (imgContourRatio > 0.25) {
				System.out.println(
						boundingRect.width() + "x" + boundingRect.height() + " " + rectRatio + " " + imgContourRatio);
				break;
			}
		}
		return contourIdx;
	}

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

	private Point[] findCorners(List<Point> listOfPoints) {

		// 0 3
		// 1 2
		int max = Integer.MAX_VALUE;

		int minX = listOfPoints.stream().mapToInt(Point::x).min().getAsInt();
		int maxX = listOfPoints.stream().mapToInt(Point::x).max().getAsInt();
		int minY = listOfPoints.stream().mapToInt(Point::y).min().getAsInt();
		int maxY = listOfPoints.stream().mapToInt(Point::y).max().getAsInt();

		// центральная точка
		int cX = minX + (maxX - minX) / 2;
		int cY = minY + (maxY - minY) / 2;

		Point[] result = new Point[] { new Point(max, max), new Point(max, 0), new Point(0, 0), new Point(0, max) };

		for (Point point : listOfPoints) {
			int x = point.x();
			int y = point.y();
			if (x < result[0].x() && y < cY) {
				result[0] = point;
			} else if (x < cX && y > result[1].y()) {
				result[1] = point;
			} else if (x > result[2].x() && y > cY) {
				result[2] = point;
			} else if (x > result[3].x() && y < cY) {
				result[3] = point;
			}
		}

		return result;
	}

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

	// перспектива
	private void perspective(Mat img_contour, Mat img) {
		Mat img_contour_gray = new Mat();
		cvtColor(img_contour, img_contour_gray, COLOR_BGR2GRAY);

		int houghThreshold = 150;
		int hough_threshold_step = 40;
		for (int i = 0; i < houghThreshold / hough_threshold_step; i++) {
			Mat lines = new Mat();
			HoughLines(img_contour_gray, lines, 2, PI / 180, houghThreshold - (i * hough_threshold_step));
			if (lines.cols() == 0)
				continue;

			ArrayList<Line> linesList = new ArrayList<>();

			FloatIndexer indexer = lines.createIndexer();
			for (int j = 0; j < indexer.cols(); j++) {
				Line line = new Line(indexer.get(0, j, 0), indexer.get(0, j, 1));
				linesList.add(line);
				// line(img, line.getPt1(), line.getPt2(), COLOR2);

				// line.getCenter();
				circle(img, line.getCenter(), 5, line.isHorizontal() ? COLOR2 : COLOR3, -1, 0, 0);
			}

			List<Line> vertical = linesList.stream().filter(Line::isVertical).collect(toList());
			List<Line> horizontal = linesList.stream().filter(Line::isHorizontal).collect(toList());

			// vertical = Line.filterCloseLines(vertical, false);
			// horizontal = Line.filterCloseLines(horizontal, true);

			System.out.println("-------");
			System.out.println(vertical);
			System.out.println(horizontal);

			Line testLine = new Line(100, (float) Math.PI / 4);
			// line(img, testLine.getPt1(), testLine.getPt2(), COLOR2);

			if (vertical.size() >= 2 && horizontal.size() >= 2) {
				for (Line line : horizontal) {
					// System.err.println(line.getTheta() + " " +
					// line.isHorizontal());
					line(img, line.getPt1(), line.getPt2(), COLOR2);
				}
				for (Line line : vertical) {
					// System.err.println(line.getTheta() + " " +
					// line.isHorizontal());
					line(img, line.getPt1(), line.getPt2(), COLOR3);
				}
				break;
			}
		}

	}

}
