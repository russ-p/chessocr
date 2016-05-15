package by.bsu.chessocr;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;

public class ImageProcessor {

	public static void main(String[] args) throws IOException {
		System.out.println("BEGIN");
		String cleanImagePath = "/home/ruslan/Projects/chessocr/2/1.png";
		// pathname = "/home/ruslan/Projects/chessocr/in.png";
		Mat cleanImg = Utils.convertBuffToMat(ImageIO.read(new File(cleanImagePath)));

		BoardExtractor boardExtractor = new BoardExtractor();
		boardExtractor.init(cleanImg);
		Mat cleanBoard = boardExtractor.extract(cleanImg);

		CellsExtractor cellsExtractor = new CellsExtractor();
		cellsExtractor.init(cleanBoard);

		Mat cleanBoardGS = Utils.toGrayscale(cleanBoard);

		String[] chessImagePaths = new String[] {
				"/home/ruslan/Projects/chessocr/2/1.png",
				"/home/ruslan/Projects/chessocr/2/2.png",
				"/home/ruslan/Projects/chessocr/2/3.png",
				"/home/ruslan/Projects/chessocr/2/4.png" };
		for (int i = 0; i < chessImagePaths.length; i++) {
			Mat chessImg = Utils.convertBuffToMat(ImageIO.read(new File(chessImagePaths[i])));
			Mat extractedBoard = boardExtractor.extract(chessImg);
			Utils.writeTestImage("out" + i + ".png", extractedBoard);

			Mat dst = new Mat();
			opencv_core.absdiff(cleanBoardGS, Utils.toGrayscale(extractedBoard), dst);
			Utils.writeTestImage("dif" + i + ".png", dst);

			for (CellId cell : CellId.chessCellIds()) {
				byte check = cellsExtractor.check(cell, extractedBoard);
				System.out.println(check);
			}
			
		}

		System.out.println("END");
	}

}
