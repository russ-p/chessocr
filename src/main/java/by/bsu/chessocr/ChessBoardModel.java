package by.bsu.chessocr;

import java.util.LinkedList;
import java.util.List;

/**
 * Электронная модель доски
 *
 */
public class ChessBoardModel {

	public static interface ChessBoardModelListener {
		void changed(CellId cellId, byte oldValue, byte newValue);
	}

	private final byte[][] board = new byte[8][8];

	private final byte[][] colors = new byte[8][8];

	private List<ChessBoardModelListener> listeners = new LinkedList<>();

	public ChessBoardModel() {
		for (byte x = 0; x < 8; x++) {
			for (byte y = 0; y < 8; y++) {
				board[x][y] = 0;
				colors[x][y] = (byte) ((x + y) % 2 == 0 ? 0 : 1);
			}
		}
	}

	public void addListener(ChessBoardModelListener listener) {
		listeners.add(listener);
	}

	public byte get(CellId cellId) {
		return board[cellId.getX()][cellId.getY()];
	}

	public boolean isWhite(CellId cellId) {
		return colors[cellId.getX()][cellId.getY()] == 0;
	}

	public void removeListener(ChessBoardModelListener listener) {
		listeners.remove(listener);
	}

	public void set(CellId cellId, byte figure) {
		byte oldValue = get(cellId);
		board[cellId.getX()][cellId.getY()] = figure;
		if (oldValue != figure) {
			for (ChessBoardModelListener listener : listeners) {
				listener.changed(cellId, oldValue, figure);
			}
		}
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		for (byte x = 0; x < 8; x++) {
			for (byte y = 0; y < 8; y++) {
				sb.append(colors[x][y] == 0 ? "." : "8");
			}
			sb.append("\n");
		}

		return sb.toString();
	}

}
