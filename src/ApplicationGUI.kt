import javafx.application.Platform
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.Stage
import javafx.stage.WindowEvent
import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * @author Thomas Povinelli
 * *         Created 2/23/17
 * *         In DiceRoller
 */
class ApplicationGUI(private val application: Application) {

    private val lock = ReentrantLock()
    private val stopEvent: EventHandler<MouseEvent>? = null
    private val singleRollButton: Button
    private val faces: ChoiceBox<Int>
    private val dice: ChoiceBox<Int>
    private val sumBox: CheckBox
    private val rollButton: Button
    private val settingBox: HBox
    private val buttonBox: HBox
    private val resultsBox: HBox
    private val labelBox: HBox
    private val mainBox: VBox
    private val writeButton: Button
    private val clearButton: Button
    var scene: Scene? = null
    private var storeStage: Stage? = null
    private val faceLabel: Label
    private val diceLabel: Label
    private val diceFaceLabels = ArrayList<Label>()
    private val storeArea = TextArea()
    private val random = Random()
    private var caughtException = false

    init {

        mainBox = VBox()
        settingBox = HBox()
        buttonBox = HBox()
        resultsBox = HBox()
        labelBox = HBox()

        faceLabel = Label("Faces:")
        diceLabel = Label("Dice:")
        singleRollButton = Button("Roll 1")
        clearButton = Button("Clear")
        clearButton.isDisable = true

        sumBox = CheckBox("Include sums in print out")

        labelBox.children.addAll(faceLabel, diceLabel)

        faces = ChoiceBox<Int>()
        for (f in faceOptions) {
            faces.items.add(f)
        }

        dice = ChoiceBox<Int>()
        for (i in 1..15) {
            dice.items.add(i)
        }

        rollButton = Button("ROLL!")
        writeButton = Button("Store")

        settingBox.children.addAll(faces, dice)
        buttonBox.children.add(writeButton)
        buttonBox.children.add(clearButton)
        buttonBox.children.add(singleRollButton)
        buttonBox.children.add(rollButton)
        mainBox.children.add(labelBox)
        mainBox.children.add(settingBox)
        mainBox.children.add(resultsBox)
        mainBox.children.add(buttonBox)
        mainBox.children.add(sumBox)

        initHandlers()
        initStyle()

        scene = Scene(mainBox, 680.0, 190.0)
    }

    private fun initHandlers() {
        application.mainStage
          .addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST) { e ->
              if (storeStage != null) {
                  storeStage!!.close()
              }
          }

        clearButton.addEventHandler(MouseEvent.MOUSE_CLICKED) { e -> storeArea.text = "" }

        sumBox.setOnAction { e ->
            if (storeArea.text.isEmpty() || caughtException) {
                return@setOnAction
            }
            val lines = storeArea.text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (sumBox.isSelected) {
                for (i in lines.indices) {
                    val rolls = lines[i]
                    var sum = 0.0
                    for (roll in rolls.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                        if (roll.chars().anyMatch { c -> !Character.isDigit(c) }) {
                            continue
                        }
                        sum += java.lang.Double.parseDouble(roll)
                    }
                    lines[i] = rolls + " Sum: " + sum
                }
                storeArea.text = lines.joinToString("\n") + "\n"
            } else {
                for (i in lines.indices) {
                    lines[i] = lines[i].replace(" ?[Ss]um:? ?[\\d.]+".toRegex(), "")
                }
                storeArea.text = lines.joinToString("\n") + "\n"
            }
        }

        writeButton.addEventHandler(MouseEvent.MOUSE_CLICKED) { e ->
            synchronized(lock) {
                if (caughtException) {
                    return@addEventHandler
                }
                clearButton.isDisable = false
                val build = StringBuilder()
                var sum = 0.0
                for (l in diceFaceLabels) {
                    build.append(l.text).append(" ")
                    sum += java.lang.Double.parseDouble(l.text)
                }
                storeArea.appendText(build.toString())

                if (sumBox.isSelected) {
                    storeArea.appendText("Sum: " + sum)
                }

                storeArea.appendText("\n")

                if (storeStage == null) {
                    storeStage = Stage()
                    storeArea.font = Font.font(FAMILY, 13.0)
                    storeStage!!.scene = Scene(storeArea, 615.0, 210.0)
                    storeStage!!.y = application.mainStage.y + 230
                    storeStage!!.x = application.mainStage.x + (530 - 465) / 2
                    storeStage!!.show()
                }
                storeStage!!.addEventFilter(
                  WindowEvent.WINDOW_CLOSE_REQUEST
                ) { it.consume() }
            }

        }

        rollButton.addEventHandler(MouseEvent.MOUSE_CLICKED
        ) { e ->
            sumBox.isDisable = true
            val bound: Int
            val r: Int
            try {
                bound = faces.value ?: throw NullPointerException()
                r = dice.value ?: throw NullPointerException()
            } catch (e1: NullPointerException) {
                caughtException = true
                this@ApplicationGUI.displayError()
                return@addEventHandler
            }

            caughtException = false

            val condition = booleanArrayOf(true)
            val stopEvent = { e11: Event ->
                condition[0] = false
                rollButton.text = "ROLL!"
                sumBox.isDisable = false
                e11.consume()
            }

            rollButton.text = "Stop!"
            rollButton.addEventFilter(MouseEvent.MOUSE_CLICKED,
                                      stopEvent)

            val t = thread(daemon = true, start = true)
            {
                while (condition[0]) {
                    Platform.runLater {
                        synchronized(lock) {
                            diceFaceLabels.clear()
                            for (j in 0..r - 1) {
                                diceFaceLabels.add(Label(
                                  (1 + random.nextInt(bound)).toString()))
                            }
                            this@ApplicationGUI.packLabels()
                        }
                    }
                    try {
                        Thread.sleep(15)
                    } catch (e1: InterruptedException) {
                        e1.printStackTrace()
                    }

                }
                rollButton.removeEventFilter(MouseEvent.MOUSE_CLICKED,
                                             stopEvent)
            }
        }

        singleRollButton.addEventHandler(MouseEvent.MOUSE_CLICKED) { e ->
            sumBox.isDisable = true
            val bound: Int
            val r: Int
            try {
                bound = this@ApplicationGUI.bound
                r = this@ApplicationGUI.r
            } catch (exception: NullPointerException) {
                this@ApplicationGUI.displayError()
                caughtException = true
                return@addEventHandler
            }

            caughtException = false

            val t = Thread {
                for (i in 0..24) {
                    synchronized(lock) {
                        diceFaceLabels.clear()
                        Platform.runLater {
                            for (j in 0..r - 1) {
                                diceFaceLabels.add(Label(
                                  (1 + random.nextInt(bound)).toString()))
                            }
                            this@ApplicationGUI.packLabels()
                        }

                    }
                    try {
                        Thread.sleep(15)
                    } catch (e1: InterruptedException) {
                        e1.printStackTrace()
                    }

                }
                sumBox.isDisable = false
            }
            t.isDaemon = true
            t.start()

        }
    }


    private fun displayError() {
        diceFaceLabels.clear()
        val wrong = Label(
          "You must select both parameters before you roll")
        wrong.textFill = Color.RED
        wrong.font = Font.font(FAMILY, FontWeight.BOLD, 14.0)
        diceFaceLabels.add(wrong)
        packErrorLabels()
    }

    private fun packErrorLabels() {
        synchronized(lock) {
            resultsBox.children.clear()
            for (l in diceFaceLabels) {
                l.font = Font.font(FAMILY, FontWeight.BOLD, 18.0)
                resultsBox.children.add(l)
            }
        }
    }

    private fun packLabels() {
        synchronized(lock) {
            resultsBox.children.clear()
            var sum = 0.0
            for (l in diceFaceLabels) {
                l.font = Font.font(FAMILY, FontWeight.BOLD, 18.0)
                sum += java.lang.Double.parseDouble(l.text)
                resultsBox.children.add(l)
            }
            val total = Label("Sum: " + sum)
            total.font = Font.font(FAMILY, FontWeight.BOLD, 18.0)
            resultsBox.children.add(total)
        }
    }

    private val bound: Int
        get() = faces.value ?: throw NullPointerException()

    private val r: Int
        get() = dice.value ?: throw NullPointerException()

    fun initStyle() {
        resultsBox.spacing = SPACING
        buttonBox.spacing = SPACING
        mainBox.spacing = SPACING
        settingBox.spacing = SPACING
        labelBox.spacing = SPACING

        labelBox.padding = Insets(PADDING)
        resultsBox.padding = Insets(PADDING)
        buttonBox.padding = Insets(PADDING)
        mainBox.padding = Insets(PADDING)
        settingBox.padding = Insets(PADDING)
        faces.prefWidth = DOUBLE_ELEMENT_WIDTH
        dice.prefWidth = DOUBLE_ELEMENT_WIDTH
        faceLabel.prefWidth = DOUBLE_ELEMENT_WIDTH
        diceLabel.prefWidth = DOUBLE_ELEMENT_WIDTH
        resultsBox.prefHeight = resultsBox.height + 30

        labelBox.alignment = ALIGNMENT
        resultsBox.alignment = ALIGNMENT
        buttonBox.alignment = ALIGNMENT
        mainBox.alignment = ALIGNMENT
        settingBox.alignment = ALIGNMENT

        application.mainStage.widthProperty().addListener { observable, oldValue, newValue ->
            resultsBox.prefWidth = newValue.toDouble() * 0.9
        }


    }

    companion object {

        val SPACING = 5.0
        val PADDING = 5.0
        val SINGLE_ELEMENT_WIDTH = 400.0
        val DOUBLE_ELEMENT_WIDTH = SINGLE_ELEMENT_WIDTH / 2 - SPACING

        val faceOptions = intArrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                                     12, 15, 16, 18, 20, 21, 22, 25, 28, 30, 35,
                                     36, 40, 42, 44, 45, 48, 49, 50, 55, 56, 60,
                                     62, 70, 72, 75, 79, 80, 81, 82, 83, 84, 85,
                                     86, 87, 88, 90, 92, 94, 95, 96, 98, 100,
                                     101, 102, 103, 105, 110, 111, 112, 116,
                                     120, 121, 122, 123, 124, 125, 130, 131,
                                     132, 135, 140, 144)
        private val ALIGNMENT = Pos.TOP_CENTER
        private val FAMILY = FontUtil.chooseFrom("Hasklig", "Source Code Pro", "Menlo", "Monaco", "Courier New").family
    }


}

private inline fun thread(daemon: Boolean, start: Boolean, crossinline function: () -> Unit): Thread {
    val t = Thread { function() }
    t.isDaemon = daemon
    if (start)
        t.start()
    return t
}
