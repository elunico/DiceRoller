import javafx.scene.text.Font;

import java.util.List;

/**
 * @author Thomas Povinelli
 * Created 3/16/17
 * In DiceRoller
 */
public class FontUtil {
    public static Font chooseFromWithSize(double size, String... families) {
        Font f = chooseFrom(families);
        return Font.font(f.getFamily(), size);
    }

    public static Font chooseFrom(String... families) {
        List<String> f = Font.getFamilies();
        for (String family : families) {
            if (f.stream().anyMatch(name -> name.contains(family))) {
                return Font.font(family);
            }
        }
        return Font.getDefault();
    }


}
