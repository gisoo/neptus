/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 15/04/2017
 */
package pt.lsts.neptus.mp.interactions;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.OperationLimits;
import pt.lsts.neptus.mp.element.IPlanElementEditorInteraction;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.coord.PolygonType.Vertex;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 *
 */
public class OperationLimitsInteraction implements IPlanElementEditorInteraction<OperationLimits> {

    private OperationLimits limits = null;
    private OperationLimits copyLimits = null;
    
    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected PolygonType polygon = new PolygonType();
    protected PolygonType.Vertex vertex = null;
    protected Point2D lastDragPoint = null;
    
    protected Color editColor = new Color(200, 200, 0, 128);
    protected Color idleColor = new Color(128, 128, 128, 224);
    
    public OperationLimitsInteraction(OperationLimits points) {
        this.limits = points;
        this.copyLimits = OperationLimits.loadXml(this.limits.asXml());
//        this.copyLimits.setEditingPainting(true);
//        this.copyLimits.setShynched(false);
        createPolygonFromLimits();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getName()
     */
    @Override
    public String getName() {
        return "Operation Limits Area";
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getIconImage()
     */
    @Override
    public Image getIconImage() {
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getMouseCursor()
     */
    @Override
    public Cursor getMouseCursor() {
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        return true;
    }

    /**
     * Given a point in the map, checks if there is some vertex intercepted.
     */
    public PolygonType.Vertex intercepted(MouseEvent evt, StateRenderer2D source) {
        for (PolygonType.Vertex v : polygon.getVertices()) {
            Point2D pt = source.getScreenPosition(new LocationType(v.getLatitudeDegs(), v.getLongitudeDegs()));
            
            if (pt.distance(evt.getPoint()) < 5)
                return v;
            
        }
        return null;
    }

    private void recalcPoints() {
        if (polygon.getVerticesSize() > 2) {
            List<Vertex> vertices = polygon.getVertices();

            double[] off1 = vertices.get(1).getLocation().getOffsetFrom(vertices.get(0).getLocation());
            double[] off2 = vertices.get(2).getLocation().getOffsetFrom(vertices.get(0).getLocation());
            double angle = vertices.get(0).getLocation().getXYAngle(vertices.get(1).getLocation()) + Math.PI / 2;
            double anglrDeg = Math.toDegrees(angle);
            double dist = MathMiscUtils.pointLineDistance(off2[0], off2[1], 0, 0, off1[0], off1[1]);

            LocationType newVt2 = new LocationType(vertices.get(0).getLocation());
            newVt2.translatePosition(Math.cos(angle) * dist, Math.sin(angle) * dist, 0);
            vertices.get(2).setLocation(newVt2);
            polygon.recomputePath();
            
            calculateLimits();
        }
        else {
            copyLimits.setArea(null);
        }
    }
    
    private void createPolygonFromLimits() {
        Double latDeg = copyLimits.getOpAreaLat();
        Double lonDeg = copyLimits.getOpAreaLon();
        Double width = copyLimits.getOpAreaWidth();
        Double length = copyLimits.getOpAreaLength();
        Double rotRads = copyLimits.getOpRotationRads();
        if (latDeg == null || !Double.isFinite(latDeg)
                || lonDeg == null || !Double.isFinite(lonDeg)
                || width == null || !Double.isFinite(width)
                || length == null || !Double.isFinite(length)
                || rotRads == null || !Double.isFinite(rotRads)) {
            polygon.clearVertices();
            return;
        }
        
        LocationType vl0 = new LocationType(latDeg, lonDeg);
        LocationType vl1 = vl0.getNewAbsoluteLatLonDepth();
        LocationType vl2 = vl0.getNewAbsoluteLatLonDepth();
        
        vl0.translatePosition(length / 2., -width / 2., 0).convertToAbsoluteLatLonDepth();
        vl1.translatePosition(length / 2., width / 2., 0).convertToAbsoluteLatLonDepth();
        vl2.translatePosition(-length / 2., -width / 2., 0).convertToAbsoluteLatLonDepth();
        
        polygon.addVertex(vl0);
        polygon.addVertex(vl1);
        polygon.addVertex(vl2);
        polygon.recomputePath();
        polygon.rotate(rotRads);
    }

    private void calculateLimits() {
        if (polygon.getVerticesSize() != 3) {
            copyLimits.setArea(null);
        }
        else {
            List<Vertex> vertices = polygon.getVertices();
            
            List<LocationType> locations = new ArrayList<>();
            vertices.forEach(v -> locations.add(v.getLocation().getNewAbsoluteLatLonDepth()));
            
            double[] off = vertices.get(1).getLocation().getOffsetFrom(vertices.get(0).getLocation());
            LocationType vloc4 = vertices.get(2).getLocation().getNewAbsoluteLatLonDepth();
            vloc4.translatePosition(off[0], off[1], 0).convertToAbsoluteLatLonDepth();
            locations.add(vloc4);
            
            LocationType centroid = CoordinateUtil.computeLocationsCentroid(locations);
            copyLimits.setOpAreaLat(centroid.getLatitudeDegs());
            copyLimits.setOpAreaLon(centroid.getLongitudeDegs());
            copyLimits.setOpAreaWidth(Math.sqrt(off[0] * off[0] + off[1] * off[1]));
            copyLimits.setOpAreaLength(vertices.get(0).getLocation().getHorizontalDistanceInMeters(vertices.get(2).getLocation()));
            copyLimits.setOpRotationRads(vertices.get(2).getLocation().getXYAngle(vertices.get(0).getLocation()));
        }
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (!SwingUtilities.isRightMouseButton(event))
            return;
        
        Vertex v = intercepted(event, source);
        JPopupMenu popup = new JPopupMenu();
        if (v != null) {
            popup.add(I18n.text("Edit location")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LocationType l = new LocationType(v.getLatitudeDegs(), v.getLongitudeDegs());
                    LocationType newLoc = LocationPanel.showLocationDialog(source, I18n.text("Edit Vertex Location"), l,
                            null, true);
                    if (newLoc != null) {
                        newLoc.convertToAbsoluteLatLonDepth();
                        v.setLocation(newLoc);
                        polygon.recomputePath();
                        recalcPoints();
                    }
                    source.repaint();
                }
            });
            popup.add(I18n.text("Remove vertex")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    polygon.removeVertex(v);
                    polygon.recomputePath();
                    recalcPoints();
                    source.repaint();
                }
            });
        }
        else if (polygon.getVerticesSize() < 3) {
            popup.add(I18n.text("Add vertex")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LocationType loc = source.getRealWorldLocation(event.getPoint());
                    // polygon.addVertex(loc.getLatitudeDegs(), loc.getLongitudeDegs());
                    polygon.addVertex(loc);
                    polygon.recomputePath();
                    recalcPoints();
                    source.repaint();
                }
            });
        }

        if (popup.getComponentCount() > 0)
            popup.show(source, event.getX(), event.getY());
    }

    private JPopupMenu createPopUpMenu(MouseEvent event, StateRenderer2D source) {
        final StateRenderer2D renderer = source;
        final Point2D mousePoint = event.getPoint();
        
        JPopupMenu popup = new JPopupMenu();
//        Point selPoint = getClickedPoint(source, event);
//        if (selPoint  == null) {
//            @SuppressWarnings("serial")
//            AbstractAction addMenu = new AbstractAction(I18n.text("Add point")) {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    LocationType locClick = renderer.getRealWorldLocation(mousePoint);
//                    copyPoints.addPoint(locClick);
//                }
//            };
//            popup.add(addMenu);
//        }
//        else {
//            @SuppressWarnings("serial")
//            AbstractAction remMenu = new AbstractAction(I18n.text("Remove point")) {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    copyPoints.removePoint(selPoint);
//                }
//            };
//            popup.add(remMenu);
//            @SuppressWarnings("serial")
//            AbstractAction copyMenu = new AbstractAction(I18n.text("Copy point")) {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    CoordinateUtil.copyToClipboard(selPoint.asLocation());
//                }
//            };
//            copyMenu.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/editcopy.png")));
//            popup.add(copyMenu);
//        }
        return popup;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mousePressed(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        Vertex v = intercepted(event, source);
        if (v != null)
            vertex = v;
        else
            adapter.mousePressed(event, source);
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseDragged(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (vertex == null) {
            if (lastDragPoint == null) {
                adapter.mouseDragged(event, source);
                lastDragPoint = event.getPoint();
                return;
            }
            else if (event.isShiftDown()) {
                double yammount = event.getPoint().getY() - lastDragPoint.getY();
                lastDragPoint = event.getPoint();
                polygon.rotate(Math.toRadians(yammount / 3));
                recalcPoints();
            }
            else if (event.isControlDown()) {
                double xammount = event.getPoint().getX() - lastDragPoint.getX();
                double yammount = event.getPoint().getY() - lastDragPoint.getY();
                lastDragPoint = event.getPoint();
                polygon.translate(-yammount / source.getZoom(), xammount / source.getZoom());
                recalcPoints();
            }
        }
        else {
            if (polygon.getVerticesSize() == 3 && polygon.getVertices().get(0) == vertex) {
//                double xammount = event.getPoint().getX() - lastDragPoint.getX();
//                double yammount = event.getPoint().getY() - lastDragPoint.getY();
//                xammount /= source.getZoom();
//                yammount /= source.getZoom();
                
                LocationType cP = source.getRealWorldLocation(event.getPoint());
                double[] off = cP.getOffsetFrom(vertex.getLocation());
                lastDragPoint = event.getPoint();
                polygon.translate(off[0], off[1]);
//                System.out.println(String.format("%f %f  %s %f " + event.getPoint(), xammount, yammount, off[0], off[1]));
            }
            else {
                vertex.setLocation(source.getRealWorldLocation(event.getPoint()));
            }
            polygon.recomputePath();
            recalcPoints();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseMoved(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseExited(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseReleased(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        if (vertex != null) {
            polygon.recomputePath();
            recalcPoints();
        }
        else {
            adapter.mouseReleased(event, source);
        }

        vertex = null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#wheelMoved(java.awt.event.MouseWheelEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#setAssociatedSwitch(pt.lsts.neptus.gui.ToolbarSwitch)
     */
    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyPressed(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyReleased(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyTyped(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#focusLost(java.awt.event.FocusEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#focusGained(java.awt.event.FocusEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#setActive(boolean, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#paintInteraction(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        AffineTransform t = g.getTransform();
        g.setTransform(source.getIdentity());

        polygon.setColor(editColor);
        polygon.setFilled(true);

        final AtomicInteger ci = new AtomicInteger(1);
        if (copyLimits != null) {
            boolean ed = limits.isEditingPainting();
            boolean sy = limits.isShynched();
            copyLimits.setEditingPainting(false);
            copyLimits.setShynched(false);
            Graphics2D g1 = (Graphics2D) g.create();
            copyLimits.paint(g1, source);
            g1.dispose();
            copyLimits.setEditingPainting(ed);
            copyLimits.setShynched(sy);
        }

        polygon.setColor(editColor);
        polygon.setFilled(false);
        if (copyLimits.getOpAreaLat() == null) {
            Graphics2D g1 = (Graphics2D) g.create();
            polygon.paint(g1, source);
            g1.dispose();
        }

        Graphics2D g2 = (Graphics2D) g.create();
        polygon.getVertices().forEach(v -> {
            Point2D pt = source.getScreenPosition(v.getLocation());
            // Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10);
            Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - 5*ci.get(), pt.getY() - 5*ci.get(), 10*ci.get(), 10*ci.getAndAdd(1));
            Color c = Color.magenta;
            g2.setColor(new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue(), 200));
            g2.fill(ellis);
            g2.setColor(c);
            g2.draw(ellis);
        });
        g2.dispose();

        g.setTransform(t);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElementEditorInteraction#getUpdatedElement()
     */
    @Override
    public OperationLimits getUpdatedElement() {
        return copyLimits;
    }
}
