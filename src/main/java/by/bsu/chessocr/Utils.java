package by.bsu.chessocr;

import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;

public class Utils {

	public static Mat toBW(Mat src) {
		// to Grayscale
		Mat img_gray = new Mat(src);
		cvtColor(new Mat(src), img_gray, COLOR_BGR2GRAY);

		// to Black and White
		Mat img_bw = new Mat(src);
		threshold(img_gray, img_bw, 128, 255, THRESH_BINARY | THRESH_OTSU);
		return img_bw;
	}

	public static Mat toGrayscale(Mat src) {
		Mat img_gray = src.clone();
		cvtColor(src, img_gray, COLOR_BGR2GRAY);
		return img_gray;
	}
	
	public static Mat toHSV(Mat src) {
		Mat img_out = src.clone();
		cvtColor(src, img_out, COLOR_RGB2HSV);
		return img_out;
	}

	public static Mat convertBuffToMat(BufferedImage img) {
		Java2DFrameConverter converter1 = new Java2DFrameConverter();
		ToMat converter2 = new OpenCVFrameConverter.ToMat();
		Mat iploriginal = converter2.convert(converter1.convert(img));
		return iploriginal;
	}

	public static BufferedImage convertMatToBuff(Mat img) {
		Java2DFrameConverter converter1 = new Java2DFrameConverter();
		OpenCVFrameConverter.ToMat converter2 = new OpenCVFrameConverter.ToMat();
		BufferedImage buff = converter1.convert(converter2.convert(img));
		return buff;
	}

	public static Mat normalize(Mat src) {
		Mat dst = new Mat(src.size(), src.type());
		org.bytedeco.javacpp.opencv_core.normalize(src, dst);
		return dst;
	}

	public static void writeTestImage(String filename, Mat region) {
		BufferedImage image = Utils.convertMatToBuff(region);
		try {
			ImageIO.write(image, "PNG", new File("out-imgs/" + filename + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static double ratio(Rect rect) {
		int w = rect.width();
		int h = rect.height();
		return ratio(w * 1d, h * 1d);
	}

	public static double ratio(double w, double h) {
		return Math.min(w, h) / Math.max(w, h);
	}
}
