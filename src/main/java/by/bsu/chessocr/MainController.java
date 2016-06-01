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
		// заполение списка веб-камер
		ObservableList<WebCamInfo> options = FXCollections.observableArrayList();
		for (Webcam webcam : Webcam.getWebcams()) {
			WebCamInfo webCamInfo = new WebCamInfo();
			webCamInfo.setWebCamIndex(webCamCounter);
			webCamInfo.setWebCamName(webcam.getName());
			options.add(webCamInfo);
			webCamCounter++;
		}

		// установка списка для выбора
		cam1Chooser.setItems(options);

		// обработчик выбора камеры
		cam1Chooser.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> {
			// устанавливаем выбранную камеру
			stream1.setWebCam(Webcam.getWebcams().get(n.getWebCamIndex()));
			// перезапускаем поток получения изображений с камеры
			stream1.restart();
		});

		// настройка размеров компонентов отображающих полученные изображения
		setImageViewSize(cam1ImgHolder, cam1Image);
		setImageViewSize(rightImgHolder, rightImg);

		cam1Image.fitHeightProperty().bind(cam1ImgHolder.heightProperty().subtract(44));
		cam1Image.fitWidthProperty().bind(cam1ImgHolder.widthProperty());

		rightImg.fitHeightProperty().bind(rightImgHolder.heightProperty().subtract(44));
		rightImg.fitWidthProperty().bind(rightImgHolder.widthProperty());

		// установка обработчки получаемых с камеры изображений
		stream1.buffImageProperty().addListener(this::processImage);

		// создание модели доски
		chessBoardModel = new ChessBoardModel();
		// создание UI-компонента, отображаещего модель доски
		chessboardComponent = new ChessboardComponent(chessBoardModel);
		chessPane.setCenter(chessboardComponent);

		// настойка компонета со списком углов поворота
		angleChoice.setItems(FXCollections.observableArrayList(0, 90, 180, 270));
		angleChoice.setValue(0);

		// обработчик изменения порогового значения
		dMean.addListener((ov, o, n) -> {
			if (n != null) {
				cellsExtractor.setdMean(n.doubleValue());
				onCapture(null);
			}
		});

		slider.valueProperty().bindBidirectional(dMean);

		dLabel.textProperty().bind(dMean.asString("%3.0f"));

		// привязка состояния кнопок Старт/Стоп к состоянию сервиса получения
		// изображения камеры
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

	// обработка полученного с камеры изображения
	private void processImage(Observable ov, BufferedImage o, BufferedImage n) {
		// конвертация в формат подходящий для отриcовки на форме
		WritableImage imgFromCam = SwingFXUtils.toFXImage(n, null);

		// если boardExtractor уже проинициалирован
		if (boardExtractor.isInitialized()) {
			// установка изображения с камеры для показа пользователю
			cam1Image.setImage(imgFromCam);
			// конвертация изображения в формат, подходящий для обработки в
			// OpenCV
			Mat chessImg = Utils.convertBuffToMat(n);
			// извлечение изображения доски из кадра, полученного с камеры
			Mat extractedBoard = boardExtractor.extract(chessImg);
			// конвертация в формат подходящий для отриcовки на форме
			BufferedImage image = Utils.convertMatToBuff(extractedBoard);
			WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
			// установка изображения доски для показа пользователю
			rightImg.setImage(fxImage);
		} else {
			// если boardExtractor не проинициалирован
			// конвертация изображения в формат, подходящий для обработки в
			// OpenCV
			Mat chessImg = Utils.convertBuffToMat(n);
			// отрисовка найденного контура на изображении с камеры
			boardExtractor.testDarawContour(chessImg);
			// конвертация в формат подходящий для отриcовки на форме
			BufferedImage image = Utils.convertMatToBuff(chessImg);
			WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
			// установка изображения доски для показа пользователю
			cam1Image.setImage(fxImage);
		}
	}

	/**
	 * ОБработчик нажатия на кнопку Инициализация
	 */
	@FXML
	public void onInitBoardExtractor(ActionEvent event) {
		// setTestImage("2/1.png");

		// если получено изображение с камеры
		if (stream1.getBuffImage() != null) {
			// конвертация изображения в формат, подходящий для обработки в
			// OpenCV
			Mat chessImg = Utils.convertBuffToMat(stream1.getBuffImage());
			// инициализируем boardExtractor
			boardExtractor.init(chessImg);

			// если на получилось проинициализировать текущим изображением,
			// показываем ошибку
			if (!boardExtractor.isInitialized()) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setHeaderText("Не удалось найти доску на изображении");
				alert.showAndWait();
				return;
			}
			// извлечение изображения доски из кадра, полученного с камеры
			Mat extractedBoard = boardExtractor.extract(chessImg);
			// инициализируем cellsExtractor
			cellsExtractor.init(extractedBoard);

			// конвертация в формат подходящий для отриcовки на форме
			BufferedImage image = Utils.convertMatToBuff(extractedBoard);
			WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
			// установка изображения доски для показа пользователю
			rightImg.setImage(fxImage);
		}
	}

	/**
	 * ОБработчик нажатия на кнопку Захват состояния
	 */
	@FXML
	public void onCapture(ActionEvent event) {
		// setTestImage("2/4.png");

		// если получено изображение с камеры
		if (stream1.getBuffImage() != null) {
			// конвертация изображения в формат, подходящий для обработки в
			// OpenCV
			Mat chessImg = Utils.convertBuffToMat(stream1.getBuffImage());
			// получечние изображения доски
			Mat extractedBoard = boardExtractor.extract(chessImg);
			// определяем состояние каждой клетки
			for (CellId cellId : CellId.chessCellIds()) {
				// результат определения сосстояния клетки - свободна/белая
				// шашка/черная шашка
				byte figure = cellsExtractor.check(cellId, extractedBoard);
				// перевод в координату модели
				cellId = cellId.rotate(angleChoice.getValue());
				// записываем значение клетки в модель
				chessBoardModel.set(cellId, figure);
			}
		}
	}

	/**
	 * ОБработчик нажатия на кнопку Захват хода
	 */
	@FXML
	public void onCaptureMove(ActionEvent event) {
		// setTestImage("2/5.png");

		// если получено изображение с камеры
		if (stream1.getBuffImage() != null) {
			// конвертация изображения в формат, подходящий для обработки в
			// OpenCV
			Mat chessImg = Utils.convertBuffToMat(stream1.getBuffImage());
			// получечние изображения доски
			Mat extractedBoard = boardExtractor.extract(chessImg);

			// запись в лог текущего времени
			logTextArea.setText(
					logTextArea.getText() + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ":\n");
			// установка обработчика изменений в модели, любое изменение теперь будет записано
			chessBoardModel.addListener(this);

			// определяем состояние каждой клетки
			for (CellId cellId : CellId.chessCellIds()) {
				byte figure = cellsExtractor.check(cellId, extractedBoard);
				// перевод в координату модели
				cellId = cellId.rotate(angleChoice.getValue());
				// записываем значение клетки в модель, если значение
				// изменилось,то будет вызван обработчик измений changed
				chessBoardModel.set(cellId, figure);
			}

			// убираем обработчик изменений
			chessBoardModel.removeListener(this);
			logTextArea.setText(logTextArea.getText() + "\n");
		}
	}

	// установка тестового изображения из файла, минуя камеру
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

	/*
	 * обработчик измений в моделе доски
	 */
	@Override
	public void changed(CellId cellId, byte oldValue, byte newValue) {
		// формируем сообщение, описывающее изменение
		String text = "";
		if (newValue == 0) // клетка стала пустой
			text = " c " + cellId;
		else // на клетке появилась шашка
			text = " на " + cellId;

		// запись в лог
		logTextArea.setText(logTextArea.getText() + text);
	}

}
