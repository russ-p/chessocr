package by.bsu.chessocr;

import java.util.ArrayList;
import java.util.List;

public class CellId {

	private static List<CellId> CHESS_CELL_IDS = null;

	private static final String[] LETTERS = new String[] { "A", "B", "C", "D", "E", "F", "G", "H" };
	private static final String[] NUMBERS = new String[] { "8", "7", "6", "5", "4", "3", "2", "1" };

	public static List<CellId> chessCellIds() {
		if (CHESS_CELL_IDS == null) {
			CHESS_CELL_IDS = new ArrayList<>(64);
			for (int x = 0; x < 8; x++) {
				for (int y = 0; y < 8; y++) {
					CHESS_CELL_IDS.add(new CellId(x, y));
				}
			}
		}
		return CHESS_CELL_IDS;
	}

	private final int x;
	private final int y;

	private CellId(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public String getLetter() {
		return LETTERS[x];
	}

	public String getNumber() {
		return NUMBERS[y];
	}

	public CellId rotate(int value) {
		switch (value) {
		case 0:
			return this;
		case 90:
			return new CellId(y, 7 - x);
		case 180:
			return new CellId(7 - y, 7 - x);
		case 270:
			return new CellId(7 - y, x);
		}
		return this;
	}

	@Override
	public String toString() {
		return getLetter() + getNumber();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CellId other = (CellId) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

}
