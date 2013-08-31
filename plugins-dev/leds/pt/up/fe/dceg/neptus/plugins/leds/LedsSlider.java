/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Aug 30, 2013
 */
package pt.up.fe.dceg.neptus.plugins.leds;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pt.up.fe.dceg.neptus.NeptusLog;

import net.miginfocom.swing.MigLayout;

/**
 * This component has a label, a slider, a text representing the desired value for brightness, and a text with the value
 * queried from imc message of the leds brightness (three,each led?).
 * Each slider is responsible to set the brightness of a group of 3 leds.
 * @author hfq
 * 
 */
public class LedsSlider extends JPanel implements ChangeListener {

    private static final long serialVersionUID = 1L;

    private JSlider slider;
    private String sliderName;

    private JLabel sliderLabel;

    // timer so that the message isn't send instantaneously
    private Timer timer;
    int delay = 1000;

    public LedsSlider(String name) {
        // super();
        this.setLayout(new MigLayout());
        this.sliderName = name;
        // this.setSize(600, 1000);
        // this.setBackground(Color.BLACK);
        this.setOpaque(false);

        createSliderContainer();
    }

    public void createSliderContainer() {
        sliderLabel = new JLabel(sliderName, JLabel.CENTER);
        // sliderLabel.setBackground(Color.DARK_GRAY);
        sliderLabel.setFont(new Font(Font.SERIF, (Font.BOLD + Font.ITALIC), 12));
        sliderLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sliderLabel.setOpaque(false);

        slider = new JSlider(JSlider.HORIZONTAL, LedsControlPanel.LED_MIN_BRIGHTNESS,
                LedsControlPanel.LED_MAX_BRIGHTNESS, LedsControlPanel.LED_INIT_BRIGHTNESS);
        slider.setValue(LedsControlPanel.LED_INIT_BRIGHTNESS);

        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setCursor(new Cursor(Cursor.HAND_CURSOR));

        slider.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));

        slider.setToolTipText(sliderName + " Brightness Controller");
        slider.setFont(new Font(Font.SERIF, Font.ITALIC, 10));
        slider.setOpaque(false);

        slider.addChangeListener(this);

        this.add(sliderLabel);
        this.add(slider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        if (!source.getValueIsAdjusting()) {
            int value = source.getValue();
            if (value == 0) {
                NeptusLog.pub().info("Send message turn off leds - brightness = 0");
            }
            else {
                NeptusLog.pub().info("Value of slider " + sliderName + " value: " + value);
            }
        }

        // JSlider source = (JSlider)e.getSource();
        // if (!source.getValueIsAdjusting()) {
        // int fps = (int)source.getValue();
        // if (fps == 0) {
        // if (!frozen) stopAnimation();
        // } else {
        // delay = 1000 / fps;
        // timer.setDelay(delay);
        // timer.setInitialDelay(delay * 10);
        // if (frozen) startAnimation();
        // }
        // }
    }
}
