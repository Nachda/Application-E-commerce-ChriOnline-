package ui.common;

import utils.LanguageManager;
import javax.swing.JFrame;

/**
 * JFrame abstraite qui écoute les changements de langue.
 * Chaque frame qui l'étend doit implémenter refreshTexts().
 */
public abstract class LanguageAwareFrame extends JFrame implements LanguageManager.LanguageChangeListener {

    public LanguageAwareFrame() {
        LanguageManager.getInstance().addLanguageChangeListener(this);
    }

    @Override
    public void onLanguageChanged() {
        refreshTexts();
    }

    public abstract void refreshTexts();
}