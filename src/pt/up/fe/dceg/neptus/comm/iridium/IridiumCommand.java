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
 * Author: zp
 * Jun 28, 2013
 */
package pt.up.fe.dceg.neptus.comm.iridium;

import java.util.Collection;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.IMCInputStream;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;

/**
 * @author zp
 *
 */
public class IridiumCommand extends IridiumMessage {

    String command;
    
    @NeptusProperty(name="Send device updates", description="may increase communications costs!")
    public boolean sendDeviceUpdates;
    
    @NeptusProperty(name="Delay, in seconds, between device updates")
    public long secondsBetweenUpdates;
    
    @NeptusProperty(name="Use Iridium hardware", description="may increase communications costs!")
    public boolean useIridium;
        
    public IridiumCommand() {
        super(2005);
    }
    
    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        out.writePlaintext(command);
        return command.getBytes("ISO-8859-1").length + 2;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        int len = in.readUnsignedShort();
        byte[] data = new byte[len];
        in.readFully(data);
        command = new String(data, "ISO-8859-1");
        return data.length + 2;
    }
    
    public final String getCommand() {
        return command;
    }

    public final void setCommand(String command) {
        this.command = command;
    }

    @Override
    public Collection<IMCMessage> asImc() {
       return new Vector<>();
    }

}
