import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Thomas Povinelli
 * Created 2/23/17
 * In DiceRoller
 */
public class AppGui {

    public static final double SPACING = 5;
    public static final double PADDING = 5;
    public static final double SINGLE_ELEMENT_WIDTH = 400;
    public static final double DOUBLE_ELEMENT_WIDTH =
      SINGLE_ELEMENT_WIDTH / 2 - SPACING;

    public static final int[] faceOptions = {
      2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 16, 18, 20, 21, 22, 25, 28, 30,
      35, 36, 40, 42, 44, 45, 48, 49, 50, 55, 56, 60, 62, 70, 72, 75, 79, 80,
      81, 82, 83, 84, 85, 86, 87, 88, 90, 92, 94, 95, 96, 98, 100, 101, 102,
      103, 105, 110, 111, 112, 116, 120, 121, 122, 123, 124, 125, 130, 131, 132,
      135, 140, 144
    };
    private static final Pos ALIGNMENT = Pos.TOP_CENTER;
    private static final String FAMILY = FontUtil.chooseFrom("SF Mono", "Menlo", "Monaco", "Consolas", "Courier New")
                                                 .getFamily();

    private final ReentrantLock lock = new ReentrantLock();
    private EventHandler<MouseEvent> stopEvent;
    private Button singleRollButton;
    private Application application;
    private ChoiceBox<Integer> faces, dice;
    private CheckBox sumBox;
    private Button rollButton;
    private HBox settingBox, buttonBox, resultsBox, labelBox;
    private VBox mainBox;
    private Button writeButton, clearButton;
    private Scene scene;
    private Stage storeStage;
    private Label faceLabel, diceLabel;
    private ArrayList<Label> diceFaceLabels = new ArrayList<>();
    private TextArea storeArea = new TextArea();
    private Random random = new Random();
    private boolean caughtException = false;

    public AppGui(Application application) {
        this.application = application;

        mainBox = new VBox();
        settingBox = new HBox();
        buttonBox = new HBox();
        resultsBox = new HBox();
        labelBox = new HBox();

        faceLabel = new Label("Faces:");
        diceLabel = new Label("Dice:");
        singleRollButton = new Button("Roll 1");
        clearButton = new Button("Clear");
        clearButton.setDisable(true);

        sumBox = new CheckBox("Include sums in print out");

        labelBox.getChildren().addAll(faceLabel, diceLabel);

        faces = new ChoiceBox<>();
        for (int f : faceOptions) {
            faces.getItems().add(f);
        }

        dice = new ChoiceBox<>();
        for (int i = 1; i < 16; i++) {
            dice.getItems().add(i);
        }

        rollButton = new Button("ROLL!");
        writeButton = new Button("Store");

        settingBox.getChildren().addAll(faces, dice);
        buttonBox.getChildren().add(writeButton);
        buttonBox.getChildren().add(clearButton);
        buttonBox.getChildren().add(singleRollButton);
        buttonBox.getChildren().add(rollButton);
        mainBox.getChildren().add(labelBox);
        mainBox.getChildren().add(settingBox);
        mainBox.getChildren().add(resultsBox);
        mainBox.getChildren().add(buttonBox);
        mainBox.getChildren().add(sumBox);

        initHandlers();
        initStyle();

        scene = new Scene(mainBox, 680, 190);
    }

    private void initHandlers() {
        application.getMainStage()
                   .addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                     e -> {
                         if (storeStage != null) {
                             storeStage.close();
                         }
                     });

        clearButton.addEventHandler(MouseEvent.MOUSE_CLICKED,
          e -> storeArea.setText(""));

        sumBox.setOnAction(e -> {
            if (storeArea.getText().isEmpty() || caughtException) {
                return;
            }
            String[] lines = storeArea.getText().split("\n");
            if (sumBox.isSelected()) {
                for (int i = 0; i < lines.length; i++) {
                    String rolls = lines[i];
                    double sum = 0;
                    for (String roll : rolls.split(" ")) {
                        if (roll.chars()
                                .anyMatch(c -> !Character.isDigit(c)))
                        {
                            continue;
                        }
                        sum += Double.parseDouble(roll);
                    }
                    lines[i] = rolls + " Sum: " + sum;
                }
                storeArea.setText(String.join("\n", lines) + "\n");
            } else {
                for (int i = 0; i < lines.length; i++) {
                    lines[i] = lines[i].replaceAll(" ?[Ss]um:? ?[\\d.]+", "");
                }
                storeArea.setText(String.join("\n", lines) + "\n");
            }
        });

        writeButton.addEventHandler(MouseEvent.MOUSE_CLICKED,
          e -> {
              synchronized (lock) {
                  if (caughtException) {
                      return;
                  }
                  clearButton.setDisable(false);
                  StringBuilder build = new StringBuilder();
                  double sum = 0;
                  for (Label l : diceFaceLabels) {
                      build.append(l.getText()).append(" ");
                      sum += Double.parseDouble(l.getText());
                  }
                  storeArea.appendText(build.toString());

                  if (sumBox.isSelected()) {
                      storeArea.appendText("Sum: " + sum);
                  }

                  storeArea.appendText("\n");

                  if (storeStage == null) {
                      storeStage = new Stage();
                      storeArea.setFont(Font.font(FAMILY, 13));
                      storeStage.setScene(new Scene(storeArea, 615, 210));
                      storeStage.setY(
                        application.getMainStage().getY() + 230);
                      storeStage.setX(application.getMainStage().getX() +
                                      (530 - 465) / 2);
                      storeStage.show();
                  }
                  storeStage.addEventFilter(
                    WindowEvent.WINDOW_CLOSE_REQUEST,
                    Event::consume);
              }

          });

        rollButton.addEventHandler(MouseEvent.MOUSE_CLICKED,
          e -> {
              sumBox.setDisable(true);
              final int bound;
              final int r;
              try {
                  bound = faces.getValue();
                  r = dice.getValue();
              } catch (NullPointerException e1) {
                  caughtException = true;
                  AppGui.this.displayError();
                  return;
              }

              caughtException = false;

              final boolean[] condition = {true};
              EventHandler<MouseEvent> stopEvent = e11 -> {
                  condition[0] = false;
                  rollButton.setText("ROLL!");
                  sumBox.setDisable(false);
                  e11.consume();
              };

              rollButton.setText("Stop!");
              rollButton.addEventFilter(MouseEvent.MOUSE_CLICKED,
                stopEvent);

              Thread t = new Thread(() -> {
                  while (condition[0]) {
                      Platform.runLater(() -> {
                          synchronized (lock) {
                              diceFaceLabels.clear();
                              for (int j = 0; j < r; j++) {
                                  diceFaceLabels.add(new Label(
                                    String.valueOf(
                                      1 + random.nextInt(bound))));
                              }
                              AppGui.this.packLabels();
                          }
                      });
                      try {
                          Thread.sleep(15);
                      } catch (InterruptedException e1) {
                          e1.printStackTrace();
                      }
                  }
                  rollButton.removeEventFilter(MouseEvent.MOUSE_CLICKED,
                    stopEvent);
              });

              t.setDaemon(true);
              t.start();

          });

        singleRollButton.addEventHandler(MouseEvent.MOUSE_CLICKED,
          e -> {
              sumBox.setDisable(true);
              final int bound;
              final int r;
              try {
                  bound = AppGui.this.getBound();
                  r = AppGui.this.getR();
              } catch (NullPointerException exception) {
                  AppGui.this.displayError();
                  caughtException = true;
                  return;
              }

              caughtException = false;

              Thread t = new Thread(() -> {
                  for (int i = 0; i < 25; i++) {
                      synchronized (lock) {
                          diceFaceLabels.clear();
                          Platform.runLater(() -> {
                              for (int j = 0; j < r; j++) {
                                  diceFaceLabels.add(new Label(
                                    String.valueOf(
                                      1 + random.nextInt(bound))));
                              }
                              AppGui.this.packLabels();
                          });

                      }
                      try {
                          Thread.sleep(15);
                      } catch (InterruptedException e1) {
                          e1.printStackTrace();
                      }
                  }
                  sumBox.setDisable(false);
              });
              t.setDaemon(true);
              t.start();

          });
    }

    private void displayError() {
        diceFaceLabels.clear();
        Label wrong = new Label(
          "You must select both parameters before you roll");
        wrong.setTextFill(Color.RED);
        wrong.setFont(Font.font(FAMILY, FontWeight.BOLD, 14));
        diceFaceLabels.add(wrong);
        packErrorLabels();
    }

    private void packErrorLabels() {
        synchronized (lock) {
            resultsBox.getChildren().clear();
            for (Label l : diceFaceLabels) {
                l.setFont(Font.font(FAMILY, FontWeight.BOLD, 18));
                resultsBox.getChildren().add(l);
            }
        }
    }

    private void packLabels() {
        synchronized (lock) {
            resultsBox.getChildren().clear();
            double sum = 0;
            for (Label l : diceFaceLabels) {
                l.setFont(Font.font(FAMILY, FontWeight.BOLD, 18));
                sum += Double.parseDouble(l.getText());
                resultsBox.getChildren().add(l);
            }
            Label total = new Label("Sum: " + sum);
            total.setFont(Font.font(FAMILY, FontWeight.BOLD, 18));
            resultsBox.getChildren().add(total);
        }
    }

    private int getBound() {
        return faces.getValue();
    }

    private int getR() {
        return dice.getValue();
    }

    public void initStyle() {
        resultsBox.setSpacing(SPACING);
        buttonBox.setSpacing(SPACING);
        mainBox.setSpacing(SPACING);
        settingBox.setSpacing(SPACING);
        labelBox.setSpacing(SPACING);

        labelBox.setPadding(new Insets(PADDING));
        resultsBox.setPadding(new Insets(PADDING));
        buttonBox.setPadding(new Insets(PADDING));
        mainBox.setPadding(new Insets(PADDING));
        settingBox.setPadding(new Insets(PADDING));
        faces.setPrefWidth(DOUBLE_ELEMENT_WIDTH);
        dice.setPrefWidth(DOUBLE_ELEMENT_WIDTH);
        faceLabel.setPrefWidth(DOUBLE_ELEMENT_WIDTH);
        diceLabel.setPrefWidth(DOUBLE_ELEMENT_WIDTH);
        resultsBox.setPrefHeight(resultsBox.getHeight() + 30);

        labelBox.setAlignment(ALIGNMENT);
        resultsBox.setAlignment(ALIGNMENT);
        buttonBox.setAlignment(ALIGNMENT);
        mainBox.setAlignment(ALIGNMENT);
        settingBox.setAlignment(ALIGNMENT);

        application.getMainStage().widthProperty().addListener(
          (observable, oldValue, newValue) -> resultsBox.setPrefWidth(
            newValue.doubleValue() * 0.9));


    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }


}
