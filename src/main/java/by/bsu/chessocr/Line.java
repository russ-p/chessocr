package by.bsu.chessocr;

import static java.lang.Math.PI;
import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bytedeco.javacpp.opencv_core.Point;

public class Line {

	private float rho;
	private float theta;
	private Point pt1;
	private Point pt2;
	private double[] center;

	public Line(float rho, float theta) {
		this.rho = rho;
		this.theta = theta;

		double a = Math.cos(theta), b = Math.sin(theta);
		double x0 = a * rho, y0 = b * rho;
		pt1 = new Point((int) round(x0 + 1000 * (-b)), (int) round(y0 + 1000 * (a)));
		pt2 = new Point((int) round(x0 - 1000 * (-b)), (int) round(y0 - 1000 * (a)));
		center = new double[] { x0, y0 };
	}

	public float getRho() {
		return rho;
	}

	public float getTheta() {
		return theta;
	}

	public Point getPt1() {
		return pt1;
	}

	public Point getPt2() {
		return pt2;
	}

	public boolean isHorizontal() {
		return theta > PI / 4.0 && theta < 3.0 * PI / 4.0;
	}

	public boolean isVertical() {
		return !isHorizontal();
	}
	

	public Point getCenter() {
		return new Point((int)center[0], (int)center[1]);
	}

	public static List<Line> filterCloseLines(List<Line> input, boolean horizontal) {
		ArrayList<Line> result = new ArrayList<>();

		Line max1 = null;
		Line max2 = null;

		input = input.stream().sorted(Comparator.comparing(Line::getTheta)).collect(Collectors.toList());
		System.out.println(horizontal ? "horizontal:" : "vertical:");
		int indx = horizontal ? 1 : 0;
		double threshold = 75;
		for (Line line1 : input) {
			for (Line line2 : input) {
				System.out.println(line1.center[indx] + "_" + line2.center[indx]);
				if (Math.abs(line1.center[indx] - line2.center[indx]) > threshold) {
					threshold = Math.abs(line1.center[indx] - line2.center[indx]);
					System.out.println(threshold);
					max1 = line1;
					max2 = line2;
				}
			}
		}
		if (max1 != null && max2 != null) {
			result.add(max1);
			result.add(max2);
		}
		return result;
	}

	@Override
	public String toString() {
		// return "Line [" + pt1.x() + "," + pt1.y() + "]-[" + pt2.x() + "," +
		// pt2.y() + "]";
		return "Line (" + center[0] + ":" + center[1] + ")";
	}


}
