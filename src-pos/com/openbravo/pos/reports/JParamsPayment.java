//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.reports;

import com.openbravo.data.loader.SerializerWrite;
import com.openbravo.pos.forms.AppView;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.List;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;

/**
 *
 * @author adrianromero
 */
public class JParamsPayment extends javax.swing.JPanel implements ReportEditorCreator {
    public JParamsPayment() {
        initComponents();     
    }

    public void init(AppView app) {
    }
        
    public void activate() throws BasicException {
    }
    
    public SerializerWrite getSerializerWrite() {
        return new SerializerWriteBasic(new Datas[] {Datas.OBJECT, Datas.STRING});
    }

    public Component getComponent() {
        return this;
    }

    
    protected void addFirst(List a) {
        // do nothing
    }
    
    public void addActionListener(ActionListener l) {
        m_jLocation.addActionListener(l);
    }
    
    public void removeActionListener(ActionListener l) {
        m_jLocation.removeActionListener(l);
    }
    
    public Object createValue() throws BasicException {
        
        try{
        Object value = Formats.STRING.parseValue(m_jLocation.getSelectedItem().toString());
        //txtField.setText(formatsvalue.formatValue(value));
        
        if (value == null||value.toString().equals("cualquiera")) {        
            return new Object[] {QBFCompareEnum.COMP_NONE, null};
        } else {
            return new Object[] {QBFCompareEnum.COMP_EQUALS, value};
        }
        }catch(NullPointerException e){
        return new Object[] {QBFCompareEnum.COMP_NONE, null};
        }
    }    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jLocation = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Pago"));

        m_jLocation.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "cualquiera", "cash", "debt" }));

        jLabel8.setText("Pago");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m_jLocation, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(m_jLocation, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(13, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel8;
    private javax.swing.JComboBox m_jLocation;
    // End of variables declaration//GEN-END:variables
    
}
