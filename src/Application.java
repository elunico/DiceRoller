import javafx.stage.Stage;

/**
 * @author Thomas Povinelli
 * Created 2/23/17
 * In DiceRoller
 */
public class Application extends javafx.application.Application {

    public static void main(String[] args) {
        launch(args);
    }

    private ApplicationGUI gui;
    private Stage mainStage;

    public Stage getMainStage() {
        return mainStage;
    }

    public ApplicationGUI getGui() {
        return gui;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;
        gui = new ApplicationGUI(this);
        primaryStage.setScene(gui.getScene());
        primaryStage.show();
    }
}
