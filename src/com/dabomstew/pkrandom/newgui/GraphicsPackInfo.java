package com.dabomstew.pkrandom.newgui;

import com.dabomstew.pkrandom.graphics.GBCImage;
import com.dabomstew.pkrandom.graphics.PlayerCharacterImages;

import javax.swing.*;
import java.awt.image.BufferedImage;

public class GraphicsPackInfo {
    private static final BufferedImage EMPTY_IMAGE = new BufferedImage(1, 1, 1);
    private static final String EMPTY_TEXT = "---";

    private JPanel ImagesPanel;
    private JPanel InfoPanel;
    private JLabel frontImageLabel;
    private final ImageIcon frontImageIcon = new ImageIcon();
    private JLabel walkSpriteLabel;
    private final ImageIcon walkSpriteIcon = new ImageIcon();
    private JLabel nameInfoLabel;
    private JLabel descriptionInfoLabel;
    private JLabel fromInfoLabel;
    private JLabel creatorInfoLabel;
    private JLabel adapterInfoLabel;
    private JLabel nameLabel;
    private JTextArea descriptionTextArea;
    private JLabel fromLabel;
    private JLabel creatorLabel;
    private JLabel adapterLabel;
    private JPanel form;

    public GraphicsPackInfo() {
        this.frontImageLabel.setIcon(frontImageIcon);
        this.walkSpriteLabel.setIcon(walkSpriteIcon);
        setGraphicsPack(null);
    }

    public void setGraphicsPack(PlayerCharacterImages pcs) { // TODO: generalize to not only player character sprites
        if (pcs == null) {
            setNullGraphicsPack();
        } else {
            GBCImage frontImage = pcs.getFrontImage();
            frontImageLabel.setVisible(frontImage != null);
            frontImageIcon.setImage(frontImage != null ? frontImage.getImage() : EMPTY_IMAGE);
            GBCImage walkSprite = pcs.getWalkSprite();
            walkSpriteLabel.setVisible(walkSprite != null);
            walkSpriteIcon.setImage(walkSprite != null ? walkSprite.getImage() : EMPTY_IMAGE);

            nameLabel.setText(pcs.getName());
            descriptionTextArea.setText(pcs.getDescription());
            fromLabel.setText(pcs.getFrom());
            creatorLabel.setText(pcs.getOriginalCreator());
            adapterLabel.setText(pcs.getAdapter());
        }
    }

    private void setNullGraphicsPack() {
        frontImageLabel.setVisible(false);
        walkSpriteLabel.setVisible(false);

        nameLabel.setText(EMPTY_TEXT);
        descriptionTextArea.setText(EMPTY_TEXT);
        fromLabel.setText(EMPTY_TEXT);
        creatorLabel.setText(EMPTY_TEXT);
        adapterLabel.setText(EMPTY_TEXT);
    }

    public void setVisible(boolean visible) {
        form.setVisible(visible);
    }

    public void setEnabled(boolean enabled) {
        form.setEnabled(enabled);
    }
}
