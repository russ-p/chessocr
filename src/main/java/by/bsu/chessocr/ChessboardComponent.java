package by.bsu.chessocr;

import java.util.HashMap;
import java.util.Map;

import by.bsu.chessocr.ChessBoardModel.ChessBoardModelListener;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class ChessboardComponent extends GridPane implements ChessBoardModelListener {

	private final Map<CellId, Shape> figures = new HashMap<>();

	public ChessboardComponent(ChessBoardModel chessBoardModel) {
		for (CellId cellId : CellId.chessCellIds()) {
			Rectangle rectangle = new Rectangle();
			rectangle.setWidth(24);
			rectangle.setHeight(24);
			rectangle.setFill(chessBoardModel.isWhite(cellId) ? Color.WHITE : Color.BLACK);

			Circle circle = new Circle(10);
			circle.setStroke(Color.RED);
			circle.setVisible(chessBoardModel.get(cellId) > 0);

			StackPane stackPane = new StackPane(rectangle, circle);
			this.add(stackPane, cellId.getX() + 1, cellId.getY() + 1);
			if (cellId.getY() == 0) {
				this.add(new Label(cellId.getLetter()), cellId.getX() + 1, 0);
			}
			if (cellId.getX() == 0) {
				this.add(new Label(cellId.getNumber()), 0, cellId.getY() + 1);
			}

			figures.put(cellId, circle);
		}

		chessBoardModel.addListener(this);
	}

	@Override
	public void changed(CellId cellId, byte oldValue, byte newValue) {
		figures.get(cellId).setVisible(newValue > 0);
		figures.get(cellId).setFill(newValue == 1 ? Color.DARKGRAY : Color.WHITE);
	}

}
