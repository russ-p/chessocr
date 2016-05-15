package by.bsu.chessocr;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

	public static void main(String[] args) throws Exception {
		launch(args);
	}

	public void start(Stage stage) throws Exception {
		String fxmlFile = "/fxml/ChessView.fxml";
		FXMLLoader loader = new FXMLLoader();
		Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlFile));

		Scene scene = new Scene(rootNode, 1200, 700);
		scene.getStylesheets().add("/styles/styles.css");

		stage.setTitle("ChessOPR");
		stage.setScene(scene);
		stage.show();
	}
}
