/*
 Copyright 2008-2012 Gephi
 Authors : Eduardo Ramos<eduramiba@gmail.com>
 Website : http://www.gephi.org

 This file is part of Gephi.

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright 2011 Gephi Consortium. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. You can obtain a copy of the License at
 http://gephi.org/about/legal/license-notice/
 or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 specific language governing permissions and limitations under the
 License.  When distributing the software, include this License Header
 Notice in each file and include the License files at
 /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
 License Header, with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"

 If you wish your version of this file to be governed by only the CDDL
 or only the GPL Version 3, indicate your decision by adding
 "[Contributor] elects to include this software in this distribution
 under the [CDDL or GPL Version 3] license." If you do not indicate a
 single choice of license, a recipient has the option to distribute
 your version of this file under either the CDDL, the GPL Version 3 or
 to extend the choice of license to its licensees as provided above.
 However, if you add GPL Version 3 code and therefore, elected the GPL
 Version 3 license, then the option applies only if the new code is
 made subject to such option by the copyright holder.

 Contributor(s):

 Portions Copyrighted 2011 Gephi Consortium.
 */
package org.gephi.dynamic.utils;

import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;


/**
 *
 * @author Eduardo Ramos<eduramiba@gmail.com>
 */
public class DynamicIntervalsParserTest {
    
    private String parseDynamic(String str) throws Exception{
        return parseDynamic(str, String.class);
    }
    
    /**
     * Temporary until we have some dynamic types representation utility like classic intervals.
     */
    private String representListAsIntervalsString(List list){
        StringBuilder sb = new StringBuilder();
        
        sb.append("<");
        for (Iterator it = list.iterator(); it.hasNext();) {
            Object object = it.next();
            sb.append(object);
            if(it.hasNext()){
                sb.append("; ");
            }
        }
        
        sb.append(">");
        
        return sb.toString();
    }
    
    private String parseDynamic(String str, Class type) throws Exception{
        if(type != null){
            System.out.println(representListAsIntervalsString(DynamicIntervalsParser.parseIntervalsWithValues(type, str)));
            return representListAsIntervalsString(DynamicIntervalsParser.parseIntervalsWithValues(type, str));
        }else{
            System.out.println(representListAsIntervalsString(DynamicIntervalsParser.parseIntervals(str)));
            return representListAsIntervalsString(DynamicIntervalsParser.parseIntervals(str));
        }
    }

    @Test
    public void testParseIntervals() throws Exception {
        assertEquals(parseDynamic("[2.0, 3.5, \"; A3R; JJG; JJG\"); [3.5, 8.0, \"; A3R; JJG; [ ] () , JJG\"]; [10,20,30]")
                , "<[2.0, 3.5, \"; A3R; JJG; JJG\"); [3.5, 8.0, \"; A3R; JJG; [ ] () , JJG\"]; [10.0, 20.0, 30]>");
        
        assertEquals(parseDynamic("<[' 2.0', '3.5', ';a b c')")
              , "<[2.0, 3.5, \";a b c\")>");
        
        assertEquals(parseDynamic(" (  1, 2,  )  (4,5, '[\\'a;b\\']']")
              , "<(1.0, 2.0); (4.0, 5.0, \"['a;b']\"]>");
        
        assertEquals(parseDynamic("[1.25,1.55, <test>]"), "<[1.25, 1.55, <test>]>");
        assertEquals(parseDynamic("[1.25,'1.55' <test>]"), "<[1.25, 1.55, <test>]>");
        
        assertEquals(parseDynamic("[1.25,1.55,   \"21.12  \"  ]", Double.class), "<[1.25, 1.55, 21.12]>");
        
        assertEquals(parseDynamic("[1.25,1.55]", Double.class), "<[1.25, 1.55]>");
        
        assertEquals(parseDynamic("[1.25,1.55]", null), "<[1.25, 1.55]>");
    }
}
