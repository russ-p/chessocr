package by.bsu.chessocr.webcam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import com.github.sarxos.webcam.Webcam;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class WebCamStreamService extends Service<Void> {

	private BufferedImage grabbedImage;
	private Webcam webCam = null;
	private boolean stopCamera = false;

	private final ObjectProperty<BufferedImage> buffImage = new SimpleObjectProperty<>();

	public void setWebCam(Webcam webCam) {
		if (this.webCam != null)
			this.webCam.close();
		this.webCam = webCam;
	}

	protected void ready() {
		stopCamera = false;
	}

	protected void cancelled() {
		stopCamera = true;
		if (webCam != null) {
			webCam.close();
			BufferedImage oldValue = getBuffImage();
			if (oldValue != null) {
				oldValue.flush();
			}
		}
	}

	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				webCam.setViewSize(new Dimension(640, 480));
				webCam.open();
				while (!stopCamera) {
					try {
						if ((grabbedImage = webCam.getImage()) != null) {
							Platform.runLater(() -> {
								BufferedImage oldValue = getBuffImage();
								if (oldValue != null) {
									oldValue.flush();
								}
								setBuffImage(grabbedImage);
							});
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				webCam.close();

				return null;
			};
		};
	}

	public final ObjectProperty<BufferedImage> buffImageProperty() {
		return this.buffImage;
	}

	public final java.awt.image.BufferedImage getBuffImage() {
		return this.buffImageProperty().get();
	}

	public final void setBuffImage(final java.awt.image.BufferedImage buffImage) {
		this.buffImageProperty().set(buffImage);
	}

}
