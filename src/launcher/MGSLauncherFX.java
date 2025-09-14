package launcher;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Properties;

public class MGSLauncherFX extends Application {

    private Clip menuThemeClip;
    private String groundZeroesPath;
    private String phantomPainPath;

    @Override
    public void start(Stage stage) {
        loadConfig();

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setFullScreen(true);

        // Disable fullscreen prompt + ESC exit
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        double screenW = screenBounds.getWidth();
        double screenH = screenBounds.getHeight();

        // Splash logo centered
        Image logoImage = new Image(getClass().getResource("/images/logo2.png").toExternalForm());
        ImageView logo = new ImageView(logoImage);
        logo.setPreserveRatio(true);
        logo.setFitWidth(screenW * 0.6);

        StackPane splashRoot = new StackPane(logo);
        splashRoot.setAlignment(Pos.CENTER);
        splashRoot.setBackground(null);

        Scene splashScene = new Scene(splashRoot, screenW, screenH, Color.TRANSPARENT);
        splashScene.setFill(Color.TRANSPARENT);

        stage.setScene(splashScene);
        stage.show();

        playAudioClip("/audio/V.wav", null);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), logo);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        PauseTransition splashDelay = new PauseTransition(Duration.seconds(3.3));
        splashDelay.setOnFinished(e -> showMainMenu(stage));
        splashDelay.play();
    }

    private void loadConfig() {
        Properties props = new Properties();
        try {
            File configFile = new File("config.properties");

            if (configFile.exists()) {
                props.load(new FileInputStream(configFile));
                groundZeroesPath = props.getProperty("ground.zeroes.path", "");
                phantomPainPath = props.getProperty("phantom.pain.path", "");
            } else {
                createDefaultConfig(configFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
            createDefaultConfig(new File("config.properties"));
        }
    }

    private void createDefaultConfig(File configFile) {
        Properties props = new Properties();
        props.setProperty("ground.zeroes.path", "C:\\Path\\To\\GroundZeroes.exe.lnk");
        props.setProperty("phantom.pain.path", "C:\\Path\\To\\PhantomPain.exe.lnk");

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "MGS Launcher Configuration\nEdit paths to your game executables or shortcuts");
            groundZeroesPath = props.getProperty("ground.zeroes.path");
            phantomPainPath = props.getProperty("phantom.pain.path");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMainMenu(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        double screenW = screenBounds.getWidth();
        double screenH = screenBounds.getHeight();

        Image bgImage = new Image(getClass().getResource("/images/background.jpg").toExternalForm());
        BackgroundImage bgImg = new BackgroundImage(
                bgImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, false, true)
        );
        Background bg = new Background(bgImg);

        StackPane root = new StackPane();
        root.setBackground(bg);

        Image logoImage = new Image(getClass().getResource("/images/logo.png").toExternalForm());
        ImageView logo = new ImageView(logoImage);
        logo.setPreserveRatio(true);
        logo.setFitWidth(screenW * 0.6);
        StackPane.setAlignment(logo, Pos.TOP_LEFT);
        StackPane.setMargin(logo, new Insets(20, 0, 0, 50));

        Button gzBtn = new Button("ACT I  : GROUND ZEROES");
        Button tppBtn = new Button("ACT II : THE PHANTOM PAIN");
        Button exitBtn = new Button("Exit");

        Font mgsFont = Font.loadFont(getClass().getResourceAsStream("/fonts/MetalGearSolid.ttf"), 24);
        if (mgsFont == null) mgsFont = Font.font("Verdana", 24);

        gzBtn.setFont(mgsFont);
        tppBtn.setFont(mgsFont);
        exitBtn.setFont(mgsFont);

        double fixedWidth = 600;
        gzBtn.setPrefWidth(fixedWidth);
        tppBtn.setPrefWidth(fixedWidth);
        exitBtn.setPrefWidth(fixedWidth);

        addHoverEffect(gzBtn);
        addHoverEffect(tppBtn);
        addHoverEffect(exitBtn);

        gzBtn.setOnAction(e -> launchGame(groundZeroesPath, stage));
        tppBtn.setOnAction(e -> launchGame(phantomPainPath, stage));
        exitBtn.setOnAction(e -> {
            if (menuThemeClip != null && menuThemeClip.isRunning()) {
                menuThemeClip.stop();
                menuThemeClip.close();
            }
            Platform.exit();
        });

        VBox firstTwo = new VBox(10, gzBtn, tppBtn);
        VBox menu = new VBox(40, firstTwo, exitBtn);
        menu.setAlignment(Pos.TOP_LEFT);
        menu.setTranslateX(70);
        menu.setTranslateY(300);

        root.getChildren().addAll(logo, menu);

        Scene mainScene = new Scene(root, screenW, screenH);
        stage.setScene(mainScene);
        stage.setFullScreen(true);

        // Disable fullscreen prompt + ESC exit again
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        menuThemeClip = playAudioClipLoop("/audio/menu_theme.wav");
    }

    private void launchGame(String gamePath, Stage stage) {
        if (gamePath == null || gamePath.trim().isEmpty()) {
            System.err.println("Game path not configured. Please edit config.properties next to the exe.");
            return;
        }

        if (menuThemeClip != null && menuThemeClip.isRunning()) {
            menuThemeClip.stop();
            menuThemeClip.close();
        }
        stage.close();

        new Thread(() -> {
            try {
                File gameFile = new File(gamePath);
                if (!gameFile.exists()) {
                    System.err.println("Game not found: " + gamePath);
                    Platform.runLater(this::relaunchLauncher);
                    return;
                }

                ProcessBuilder pb;
                if (gamePath.toLowerCase().endsWith(".lnk")) {
                    pb = new ProcessBuilder("explorer.exe", gameFile.getAbsolutePath());
                } else {
                    pb = new ProcessBuilder(gamePath);
                    pb.directory(gameFile.getParentFile());
                }

                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Launch output: " + line);
                    }
                }

                Thread.sleep(2000);
                Platform.runLater(this::relaunchLauncher);

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(this::relaunchLauncher);
            }
        }).start();
    }

    private void relaunchLauncher() {
        try {
            String jarPath = MGSLauncherFX.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();

            ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarPath);
            pb.start();

            Platform.exit();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                try {
                    new MGSLauncherFX().start(new Stage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    private void addHoverEffect(Button btn) {
        String normalStyle =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: black; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-alignment: center-left; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-radius: 0;";

        String hoverStyle =
                "-fx-background-color: black; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-alignment: center-left; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-radius: 0;";

        String pressedStyle =
                "-fx-background-color: #1a1a1a; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-alignment: center-left; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-radius: 0;";

        btn.setStyle(normalStyle);

        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            playHoverSound();
        });

        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));

        btn.setOnMousePressed(e -> {
            btn.setStyle(pressedStyle);
            btn.setTranslateY(2);
        });

        btn.setOnMouseReleased(e -> {
            btn.setTranslateY(0);
            btn.setStyle(hoverStyle);
        });
    }

    private void playHoverSound() {
        new Thread(() -> {
            try (InputStream rawStream = getClass().getResourceAsStream("/audio/hover.wav");
                 BufferedInputStream bis = new BufferedInputStream(rawStream)) {

                AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Clip playAudioClip(String resourcePath, Runnable onEnd) {
        new Thread(() -> {
            try (InputStream rawStream = getClass().getResourceAsStream(resourcePath);
                 BufferedInputStream bis = new BufferedInputStream(rawStream)) {

                AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        if (onEnd != null) Platform.runLater(onEnd);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return null;
    }

    private Clip playAudioClipLoop(String resourcePath) {
        try (InputStream rawStream = getClass().getResourceAsStream(resourcePath);
             BufferedInputStream bis = new BufferedInputStream(rawStream)) {

            AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
