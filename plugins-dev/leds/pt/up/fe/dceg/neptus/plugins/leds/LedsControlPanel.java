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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.QueryLedBrightness;
import pt.up.fe.dceg.neptus.imc.SetLedBrightness;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * This panel is responsible for controlling the Leds brightness placed on Adamastor.
 * 
 * @author hfq
 * 
 *         FIX ME - Change panel icon
 * 
 *         There are 4 groups of leds, each one containing 3 leds, placed on the front of the Rov
 * 
 *         Use: IMC::SetLedBrightness IMC::QueryLedBrightness
 * 
 *         IMC::LedBrightness
 * 
 *         SetLedBrightness extends IMCMessage
 * 
 *         QueryLedBrightness extends IMCMessage will reply with LedBrightness
 * 
 *         LED4R - device that allows controlling up to 12 high-brightness LEDs [Actuators.LED4R] Enabled = Hardware
 *         Entity Label = LED Driver Serial Port - Device = /dev/ttyUSB3 LED - Names = LED0, LED1, LED2, LED3, LED4,
 *         LED5, LED6, LED7, LED8, LED9, LED10, LED11
 * 
 */
// @Popup(pos = POSITION.TOP_LEFT, accelerator = 'D')
@Popup(pos = POSITION.TOP_LEFT, width = 400, height = 400, accelerator = 'D')
@PluginDescription(author = "hfq", description = "Panel that enables setting up leds brightness", name = "Leds Control Panel", version = "0.1", icon = "images/menus/tip.png")
public class LedsControlPanel extends SimpleSubPanel implements ActionListener {
    private static final long serialVersionUID = 1L;
    private ConsoleLayout console;

    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 400;

    // Leds Brightness in percentage / max brightness value = 255
    protected static final int LED_MIN_BRIGHTNESS = 0;
    protected static final int LED_MAX_BRIGHTNESS = 100;
    protected static final int LED_INIT_BRIGHTNESS = 0;

    public static final String[] ledNames = { "LED0", "LED1", "LED2", "LED3", "LED4", "LED5", "LED6", "LED7", "LED8",
            "LED9", "LED10", "LED11", "LED12" };

    public LinkedHashMap<String, Integer> msgLeds = new LinkedHashMap<>();

    private LedsSlider slider1, slider2, slider3, slider4;

    // Can have a timer to turn on the 4 groups of leds (3 leds per group) in a clockwise matter
    // Timer time;

    /**
     * @param console
     */
    public LedsControlPanel(ConsoleLayout console) {
        super(console);
        this.console = console;
        this.setLayout(new MigLayout("insets 0"));
        this.removeAll();
        // this.setBackground(Color.DARK_GRAY);
        this.setOpaque(true);
        this.setResizable(true);
        // this.console.addMainVehicleListener(this);
        ImcMsgManager.getManager().addListener(this);

        initMsgMapping();
    }

    /**
     * Fill up leds mapping
     */
    private void initMsgMapping() {
        for (int i = 0; i < 12; ++i) {
            // msgLeds.put("LED" + (i), 0);
            msgLeds.put(ledNames[0], 0);
        }
    }

    /**
     * 
     */
    private void createPanel() {
        slider1 = new LedsSlider("Leds G1 ");
        slider2 = new LedsSlider("Leds G2 ");
        slider3 = new LedsSlider("Leds G3 ");
        slider4 = new LedsSlider("Leds G4 ");
        this.add(slider1, "wrap");
        this.add(slider2, "wrap");
        this.add(slider3, "wrap");
        this.add(slider4, "wrap");

        SetLedBrightness msgLed1 = new SetLedBrightness();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        LedsControlPanel lcp = new LedsControlPanel(null);
        // lcp.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        lcp.printMsgMapping();
        lcp.createPanel();
        GuiUtils.testFrame(lcp, "Test" + lcp.getClass().getSimpleName(), PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphic2d = (Graphics2D) g;
        Color color1 = getBackground();
        Color color2 = color1.darker();
        Color color3 = Color.BLACK;
        GradientPaint gradpaint = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color3);
        graphic2d.setPaint(gradpaint);
        graphic2d.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override
    public void initSubPanel() {
        createPanel();
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    /**
    *
    */
    private void printMsgMapping() {
        NeptusLog.pub().info("Led brightness class id: " + SetLedBrightness.ID_STATIC);
        NeptusLog.pub().info("Query Led class id  " + QueryLedBrightness.ID_STATIC);
        for (Entry<String, Integer> entry : msgLeds.entrySet()) {
            NeptusLog.pub().info("Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
        // // Finally send the message
        // RemoteActions msg = new RemoteActions();
        // msg.setActions(msgActions);
        // ImcMsgManager.getManager().sendMessageToSystem(msg, console.getMainSystem());
    }
}
