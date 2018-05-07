/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * May 5, 2018
 */
package pt.lsts.neptus.endurance;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;

import pt.lsts.aismanager.ShipAisSnapshot;
import pt.lsts.aismanager.api.AisContactManager;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="SOI Situation Awareness")
public class SoiAwareness extends ConsoleInteraction {

    @NeptusProperty(name="Future ship track size", units="s")
    public int shipTrackSize = 1200;
        
    @NeptusProperty(name="Paint ship names")
    public boolean paintShipNames = true;
    
    
    
    private JSlider timeSlider;
    private Date curTime = new Date();
    private int hourParts = 60;
    private int timeDiff = 12;
    private Font dateFont = new Font("Helvetica", Font.BOLD, 18);
    private GeneralPath vehShape = new GeneralPath();
    private GeneralPath shipShape = new GeneralPath();
    
    public SoiAwareness() {
        LookAndFeel prev = UIManager.getLookAndFeel();
        GuiUtils.setLookAndFeelNimbus();
        this.timeSlider = new JSlider(-timeDiff * hourParts, timeDiff * hourParts, 0);
        Dictionary<Integer, JLabel> labels = new Hashtable<>();
        for (int i = -timeDiff; i <= timeDiff; i++)
            labels.put(i*hourParts, new JLabel(""+i));
        timeSlider.setMajorTickSpacing(hourParts);        
        timeSlider.setPaintTicks(true);
        
        timeSlider.setLabelTable(labels);
        timeSlider.setPaintLabels(true);
        timeSlider.setSnapToTicks(false);
        timeSlider.repaint();
        timeSlider.addChangeListener(this::timeChanged);
        try {
            UIManager.setLookAndFeel(prev);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        timeSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                timeSlider.setValue(0);
            }
            
        });        
        
        vehShape.moveTo(-7, 4);
        vehShape.lineTo(0, -12);
        vehShape.lineTo(7, 4);
        vehShape.lineTo(0, 0);
        vehShape.closePath();
        
        shipShape.moveTo(-10, 4);
        shipShape.lineTo(0, -12);
        shipShape.lineTo(10, 4);
        shipShape.lineTo(0, 0);
        shipShape.closePath();
        
    }
    
    public void timeChanged(ChangeEvent evt) {        
        update();
    }
    
    @Periodic(millisBetweenUpdates=1000)
    public void update() {
        long timeDiff = (long) (3600 * 1000.0 *(timeSlider.getValue() / (float)hourParts));
        this.curTime = new Date(System.currentTimeMillis() + timeDiff);        
        
    }
    
    @Override
    public void initInteraction() {
        
    }

    @Override
    public void cleanInteraction() {

    }
    
  
    @Override
    public void setActive(boolean active, StateRenderer2D source) {
        super.setActive(active, source);
        Container parent = source.getParent();
        while (parent != null && !(parent.getLayout() instanceof BorderLayout)) 
            parent = parent.getParent();
        if (active) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(timeSlider);
            parent.add(panel, BorderLayout.SOUTH);
        }
        else {
            parent = timeSlider.getParent().getParent();
            parent.remove(timeSlider.getParent());
        }
        parent.invalidate();
        parent.validate();
        parent.repaint();
    }
    
    @Override
    public void paintInteraction(Graphics2D g2, StateRenderer2D source) {
        super.paintInteraction(g2, source);
        if (!isActive())
            return;
        
        if (timeSlider.getValue() == 0)
            return;
        
        Graphics2D g = (Graphics2D)g2.create();
        g.setColor(new Color(64, 64, 64, 128));
        g.fillRect(0, 0, source.getWidth(), source.getHeight());        
         
        g.setFont(dateFont);
        g.setColor(Color.white);
        
        String text = ""+curTime; 
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text , (source.getWidth()/2) - w/2, source.getHeight()-10);
        
        
        for (Asset asset : AssetsManager.getInstance().getAssets()) {
            AssetState state = asset.stateAt(curTime);
            
            VehicleType vehicle = VehiclesHolder.getVehicleById(asset.getAssetName());
            if (state != null)
                paintAssetState(vehicle, state, (Graphics2D)g2.create(), source);
        }
        HashMap<String, ShipAisSnapshot> ships = AisContactManager.getInstance().getFutureSnapshots(curTime.getTime() - System.currentTimeMillis());
        
        for (Entry<String, ShipAisSnapshot> entry : ships.entrySet()) {
            paintShipState(entry.getKey(), entry.getValue(), (Graphics2D)g2.create(), source);
        }
    }
    
    private void paintShipState(String name, ShipAisSnapshot state, Graphics2D g, StateRenderer2D renderer) {
        LocationType loc = new LocationType(state.getLatDegs(), state.getLonDegs());
        Point2D pt = renderer.getScreenPosition(loc);
        
        g.translate(pt.getX(), pt.getY());  
        
        if (paintShipNames) {
            g.setColor(new Color(0, 0, 0));
            g.drawString(name, 5, 5);
        }        
        
        g.rotate(state.getCog()-renderer.getRotation());

        if (shipTrackSize > 0) {
            g.setColor(Color.RED.darker());
            g.setPaint(new GradientPaint(0f, 0f, Color.red.darker(), 0f, (float)(-renderer.getZoom() * shipTrackSize * state.getSogMps()), new Color(255,0,0,0)));
            g.draw(new Line2D.Double(0,-renderer.getZoom() * shipTrackSize * state.getSogMps(), 0, 0));
        }
        
        g.setColor(Color.red.darker());
        g.fill(shipShape);
        g.setColor(Color.black);
        g.draw(shipShape);
    }
    
    private void paintAssetState(VehicleType vehicle, AssetState state, Graphics2D g, StateRenderer2D renderer) {
        LocationType loc = new LocationType(state.getLatitude(), state.getLongitude());
        Point2D pt = renderer.getScreenPosition(loc);
        
        if (vehicle != null)
            g.setColor(vehicle.getIconColor());
        else
            g.setColor(Color.red);
        
        g.translate(pt.getX(), pt.getY());        
        g.rotate(Math.toRadians(state.getHeading())-renderer.getRotation());
        
        g.fill(vehShape);
        g.setColor(Color.white);
        g.draw(vehShape);
    }
    
    public static void main(String[] args) {
        GuiUtils.setLookAndFeelNimbus();
        JSlider slider = new JSlider(-240, 240, 0);
        Dictionary<Integer, JLabel> labels = new Hashtable<>();
        for (int i = -12; i <= 12; i++)
            labels.put(i*20, new JLabel(""+i));
        slider.setMajorTickSpacing(20);        
        slider.setPaintTicks(true);        
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(false);
        GuiUtils.testFrame(slider);
    }
}
