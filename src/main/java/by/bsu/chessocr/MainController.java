package by.bsu.chessocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.Mat;

import com.github.sarxos.webcam.Webcam;

import by.bsu.chessocr.ChessBoardModel.ChessBoardModelListener;
import by.bsu.chessocr.webcam.WebCamInfo;
import by.bsu.chessocr.webcam.WebCamStreamService;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class MainController implements Initializable, ChessBoardModelListener {

	@FXML
	private ChoiceBox<WebCamInfo> cam1Chooser;

	@FXML
	private ImageView cam1Image;

	@FXML
	private BorderPane cam1ImgHolder;

	@FXML
	private ImageView rightImg;

	@FXML
	private BorderPane rightImgHolder;

	@FXML
	BorderPane chessPane;

	@FXML
	private ChoiceBox<Integer> angleChoice;

	@FXML
	TextArea logTextArea;

	private WebCamStreamService stream1 = new WebCamStreamService();

	private BoardExtractor boardExtractor = new BoardExtractor();

	private CellsExtractor cellsExtractor = new CellsExtractor();

	private ChessboardComponent chessboardComponent;

	private ChessBoardModel chessBoardModel;

	private final DoubleProperty dMean = new SimpleDoubleProperty(cellsExtractor.getdMean());

	@FXML
	Slider slider;

	@FXML
	Label dLabel;

	@FXML
	Button startBtn;

	@FXML
	Button stopBtn;

	@FXML
	void startCam1(ActionEvent event) {
		stream1.restart();
	}

	@FXML
	void stopCam1(ActionEvent event) {
		stream1.cancel();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		int webCamCounter = 0;
		ObservableList<WebCamInfo> options = FXCollections.observableArrayList();
		for (Webcam webcam : Webcam.getWebcams()) {
			WebCamInfo webCamInfo = new WebCamInfo();
			webCamInfo.setWebCamIndex(webCamCounter);
			webCamInfo.setWebCamName(webcam.getName());
			options.add(webCamInfo);
			webCamCounter++;
		}

		cam1Chooser.setItems(options);

		cam1Chooser.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> {
			stream1.setWebCam(Webcam.getWebcams().get(n.getWebCamIndex()));
			stream1.restart();
		});

		setImageViewSize(cam1ImgHolder, cam1Image);
		setImageViewSize(rightImgHolder, rightImg);

		cam1Image.fitHeightProperty().bind(cam1ImgHolder.heightProperty().subtract(44));
		cam1Image.fitWidthProperty().bind(cam1ImgHolder.widthProperty());

		rightImg.fitHeightProperty().bind(rightImgHolder.heightProperty().subtract(44));
		rightImg.fitWidthProperty().bind(rightImgHolder.widthProperty());

		stream1.buffImageProperty().addListener(this::processImage);

		chessBoardModel = new ChessBoardModel();
		chessboardComponent = new ChessboardComponent(chessBoardModel);
		chessPane.setCenter(chessboardComponent);

		angleChoice.setItems(FXCollections.observableArrayList(0, 90, 180, 270));
		angleChoice.setValue(0);

		dMean.addListener((ov, o, n) -> {
			if (n != null) {
				cellsExtractor.setdMean(n.doubleValue());
				onCapture(null);
			}
		});

		slider.valueProperty().bindBidirectional(dMean);

		dLabel.textProperty().bind(dMean.asString("%3.0f"));
		
		stopBtn.disableProperty().bind(stream1.stateProperty().isNotEqualTo(State.RUNNING));
		startBtn.disableProperty().bind(stream1.stateProperty().isEqualTo(State.RUNNING));
	}

	protected void setImageViewSize(Pane imgHolder, ImageView camImage) {
		double height = imgHolder.getHeight();
		double width = imgHolder.getWidth();
		camImage.setFitHeight(height);
		camImage.setFitWidth(width);
		camImage.prefHeight(height);
		camImage.prefWidth(width);
		camImage.setPreserveRatio(true);
	}

	@FXML
	void onShow(ActionEvent event) {

	}

	private void processImage(Observable ov, BufferedImage o, BufferedImage n) {
		WritableImage imgFromCam = SwingFXUtils.toFXImage(n, null);

		if (boardExtractor.isInitialized()) {
			cam1Image.setImage(imgFromCam);
			Mat chessImg = Utils.convertBuffToMat(n);
			Mat extractedBoard = boardExtractor.extract(chessImg);
			extractedBoard = Utils.toGrayscale(extractedBoard);
			BufferedImage image = Utils.convertMatToBuff(extractedBoard);
			WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
			rightImg.setImage(fxImage);
		} else {
			Mat chessImg = Utils.convertBuffToMat(n);
			boardExtractor.testDarawContour(chessImg);
			BufferedImage image = Utils.convertMatToBuff(chessImg);
			WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
			cam1Image.setImage(fxImage);
		}
	}

	@FXML
	public void onInitBoardExtractor(ActionEvent event) {
		setTestImage("/home/ruslan/Projects/chessocr/2/1.png");

		if (stream1.getBuffImage() != null) {
			Mat chessImg = Utils.convertBuffToMat(stream1.getBuffImage());
			boardExtractor.init(chessImg);

			if (!boardExtractor.isInitialized()) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setHeaderText("Не удалось найти доску на изображении");
				alert.showAndWait();
				return;
			}

			Mat extractedBoard = boardExtractor.extract(chessImg);
			cellsExtractor.init(extractedBoard);

			extractedBoard = Utils.toGrayscale(extractedBoard);
			BufferedImage image = Utils.convertMatToBuff(extractedBoard);
			WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
			rightImg.setImage(fxImage);
		}
	}

	@FXML
	public void onCapture(ActionEvent event) {
		setTestImage("/home/ruslan/Projects/chessocr/2/4.png");

		if (stream1.getBuffImage() != null) {
			Mat chessImg = Utils.convertBuffToMat(stream1.getBuffImage());
			Mat extractedBoard = boardExtractor.extract(chessImg);
			for (CellId cellId : CellId.chessCellIds()) {
				byte figure = cellsExtractor.check(cellId, extractedBoard);
				// перевод в координату модели
				cellId = cellId.rotate(angleChoice.getValue());
				chessBoardModel.set(cellId, figure);
			}
		}
	}

	@FXML
	public void onCaptureMove(ActionEvent event) {
		setTestImage("/home/ruslan/Projects/chessocr/2/5.png");

		if (stream1.getBuffImage() != null) {
			Mat chessImg = Utils.convertBuffToMat(stream1.getBuffImage());
			Mat extractedBoard = boardExtractor.extract(chessImg);

			logTextArea.setText(
					logTextArea.getText() + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ":\n");
			chessBoardModel.addListener(this);

			for (CellId cellId : CellId.chessCellIds()) {
				byte figure = cellsExtractor.check(cellId, extractedBoard);
				// перевод в координату модели
				cellId = cellId.rotate(angleChoice.getValue());
				chessBoardModel.set(cellId, figure);
			}

			chessBoardModel.removeListener(this);
			logTextArea.setText(logTextArea.getText() + "\n");
		}
	}

	// установка тестового изображения из файла
	@SuppressWarnings("unused")
	private void setTestImage(String imagePath) {
		try {
			BufferedImage bufferedImage = null;
			bufferedImage = ImageIO.read(new File(imagePath));
			stream1.setBuffImage(bufferedImage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void changed(CellId cellId, byte oldValue, byte newValue) {
		String text = "";
		if (newValue == 0)
			text = " c " + cellId;
		else
			text = " на " + cellId;

		logTextArea.setText(logTextArea.getText() + text);
	}

}
