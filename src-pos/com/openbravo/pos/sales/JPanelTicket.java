package com.openbravo.pos.sales;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Date;

import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.printer.*;

import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.scale.ScaleException;
import com.openbravo.pos.payment.JPaymentSelect;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ListKeyed;
import com.openbravo.data.loader.DataParams;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.SerializerWriteParams;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.customers.JCustomerFinder;
import com.openbravo.pos.customers.JCustomerFinder1;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.inventory.TaxCategoryInfo;
import com.openbravo.pos.payment.JPaymentSelectReceipt;
import com.openbravo.pos.payment.JPaymentSelectRefund;
import com.openbravo.pos.payment.PaymentInfo;
import com.openbravo.pos.payment.PaymentInfoFree;
import com.openbravo.pos.scale.ScaleDialog;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.util.JRPrinterAWT300;
import com.openbravo.pos.util.ReportUtils;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintService;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapArrayDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/**
 *
 * @author adrianromero
 */
public abstract class JPanelTicket extends JPanel implements JPanelView, BeanFactoryApp, TicketsEditor {
   
    // Variable numerica
    private final static int NUMBERZERO = 0;
    private final static int NUMBERVALID = 1;
    
    private final static int NUMBER_INPUTZERO = 0;
    private final static int NUMBER_INPUTZERODEC = 1;
    private final static int NUMBER_INPUTINT = 2;
    private final static int NUMBER_INPUTDEC = 3; 
    private final static int NUMBER_PORZERO = 4; 
    private final static int NUMBER_PORZERODEC = 5; 
    private final static int NUMBER_PORINT = 6; 
    private final static int NUMBER_PORDEC = 7; 

    protected JTicketLines m_ticketlines;
        
    // private Template m_tempLine;
    private TicketParser m_TTP;
    
    protected TicketInfo m_oTicket; 
    protected Object m_oTicketExt; 
    
    // Estas tres variables forman el estado...
    private int m_iNumberStatus;
    private int m_iNumberStatusInput;
    private int m_iNumberStatusPor;
    private StringBuffer m_sBarcode;
            
    private JTicketsBag m_ticketsbag;
    
    private SentenceList senttax;
    private ListKeyed taxcollection;
    // private ComboBoxValModel m_TaxModel;
    
    private SentenceList senttaxcategories;
    private ListKeyed taxcategoriescollection;
    private ComboBoxValModel taxcategoriesmodel;
    
    private TaxesLogic taxeslogic;
    
//    private ScriptObject scriptobjinst;
    protected JPanelButtons m_jbtnconfig;
    
    protected AppView m_App;
    protected DataLogicSystem dlSystem;
    protected DataLogicSales dlSales;
    protected DataLogicCustomers dlCustomers;
    
    private JPaymentSelect paymentdialogreceipt;
    private JPaymentSelect paymentdialogrefund;

    /** Creates new form JTicketView */
    public JPanelTicket() {
        
        initComponents ();
    }
   
    public void init(AppView app) throws BeanFactoryException {
        
        m_App = app;
        dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
        dlSales = (DataLogicSales) m_App.getBean("com.openbravo.pos.forms.DataLogicSales");
        dlCustomers = (DataLogicCustomers) m_App.getBean("com.openbravo.pos.customers.DataLogicCustomers");
                    
        // borramos el boton de bascula si no hay bascula conectada
        if (!m_App.getDeviceScale().existsScale()) {
            m_jbtnScale.setVisible(false);
        }
        
        m_ticketsbag = getJTicketsBag();
        m_jPanelBag.add(m_ticketsbag.getBagComponent(), BorderLayout.LINE_START);
        add(m_ticketsbag.getNullComponent(), "null");

        m_ticketlines = new JTicketLines(dlSystem.getResourceAsXML("Ticket.Line"));
        m_jPanelCentral.add(m_ticketlines, java.awt.BorderLayout.CENTER);
        
        m_TTP = new TicketParser(m_App.getDeviceTicket(), dlSystem);
               
        // Los botones configurables...
        m_jbtnconfig = new JPanelButtons("Ticket.Buttons", this);
        m_jButtonsExt.add(m_jbtnconfig);           
       
        // El panel de los productos o de las lineas...        
        catcontainer.add(getSouthComponent(), BorderLayout.CENTER);
        
        // El modelo de impuestos
        senttax = dlSales.getTaxList();
        senttaxcategories = dlSales.getTaxCategoriesList();
        
        taxcategoriesmodel = new ComboBoxValModel();    
              
        // ponemos a cero el estado
        stateToZero();  
        
        // inicializamos
        m_oTicket = null;
        m_oTicketExt = null;      
    }
    
    public Object getBean() {
        return this;
    }
    
    public JComponent getComponent() {
        return this;
    }

    public void activate() throws BasicException {

        paymentdialogreceipt = JPaymentSelectReceipt.getDialog(this);
        paymentdialogreceipt.init(m_App);
        paymentdialogrefund = JPaymentSelectRefund.getDialog(this); 
        paymentdialogrefund.init(m_App);
        
        // impuestos incluidos seleccionado ?
        m_jaddtax.setSelected("true".equals(m_jbtnconfig.getProperty("taxesincluded")));

        // Inicializamos el combo de los impuestos.
        java.util.List<TaxInfo> taxlist = senttax.list();
        taxcollection = new ListKeyed<TaxInfo>(taxlist);
        java.util.List<TaxCategoryInfo> taxcategorieslist = senttaxcategories.list();
        taxcategoriescollection = new ListKeyed<TaxCategoryInfo>(taxcategorieslist);
        
        taxcategoriesmodel = new ComboBoxValModel(taxcategorieslist);
        m_jTax.setModel(taxcategoriesmodel);

        String taxesid = m_jbtnconfig.getProperty("taxcategoryid");
        if (taxesid == null) {
            if (m_jTax.getItemCount() > 0) {
                m_jTax.setSelectedIndex(0);
            }
        } else {
            taxcategoriesmodel.setSelectedKey(taxesid);
        }              
                
        taxeslogic = new TaxesLogic(taxlist);
        
        // Show taxes options
        if (m_App.getAppUserView().getUser().hasPermission("sales.ChangeTaxOptions")) {
            m_jTax.setVisible(true);
            m_jaddtax.setVisible(true);
        } else {
            m_jTax.setVisible(false);
            m_jaddtax.setVisible(false);
        }
        
        // Authorization for buttons
        btnSplit.setEnabled(m_App.getAppUserView().getUser().hasPermission("sales.Total"));
        m_jDelete.setEnabled(m_App.getAppUserView().getUser().hasPermission("sales.EditLines"));
        m_jNumberKeys.setMinusEnabled(m_App.getAppUserView().getUser().hasPermission("sales.EditLines"));
        m_jNumberKeys.setEqualsEnabled(m_App.getAppUserView().getUser().hasPermission("sales.Total"));
        m_jbtnconfig.setPermissions(m_App.getAppUserView().getUser());  
               
        m_ticketsbag.activate();        
    }
    
    public boolean deactivate() {

        return m_ticketsbag.deactivate();
    }
    
    protected abstract JTicketsBag getJTicketsBag();
    protected abstract Component getSouthComponent();
    protected abstract void resetSouthComponent();
     
    public void setActiveTicket(TicketInfo oTicket, Object oTicketExt) {
       
        m_oTicket = oTicket;
        m_oTicketExt = oTicketExt;
        
        if (m_oTicket != null) {            
            // Asign preeliminary properties to the receipt
            m_oTicket.setUser(m_App.getAppUserView().getUser().getUserInfo());
            m_oTicket.setActiveCash(m_App.getActiveCashIndex());
            m_oTicket.setDate(new Date()); // Set the edition date.
        }
        
        executeEvent(m_oTicket, m_oTicketExt, "ticket.show");
        
        refreshTicket();               
    }
    
    public TicketInfo getActiveTicket() {
        return m_oTicket;
    }
    
    private void refreshTicket() {
        
        CardLayout cl = (CardLayout)(getLayout());
        
        if (m_oTicket == null) {        
            m_jTicketId.setText(null);            
            m_ticketlines.clearTicketLines();
           
            m_jSubtotalEuros.setText(null);
            m_jTaxesEuros.setText(null);
            m_jTotalEuros.setText(null); 
        
            stateToZero();
            
            // Muestro el panel de nulos.
            cl.show(this, "null");  
            resetSouthComponent();

        } else {
            if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                //Make disable Search and Edit Buttons
                m_jEditLine.setVisible(false);
                m_jList.setVisible(false);
            }
            
            // Refresh ticket taxes
            for (TicketLineInfo line : m_oTicket.getLines()) {
                line.setTaxInfo(taxeslogic.getTaxInfo(line.getProductTaxCategoryID(), m_oTicket.getDate(), m_oTicket.getCustomer()));
            }  
        
            // The ticket name
            m_jTicketId.setText(m_oTicket.getName(m_oTicketExt));

            // Limpiamos todas las filas y anadimos las del ticket actual
            m_ticketlines.clearTicketLines();

            for (int i = 0; i < m_oTicket.getLinesCount(); i++) {
                m_ticketlines.addTicketLine(m_oTicket.getLine(i));
            }
            printPartialTotals();
            stateToZero();
            
            // Muestro el panel de tickets.
            cl.show(this, "ticket");
            resetSouthComponent();
            
            // activo el tecleador...
            m_jKeyFactory.setText(null);       
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    m_jKeyFactory.requestFocus();
                }
            });
        }
    }
       
    private void printPartialTotals(){
               
        if (m_oTicket.getLinesCount() == 0) {
            m_jSubtotalEuros.setText(null);
            m_jTaxesEuros.setText(null);
            m_jTotalEuros.setText(null);
        } else {
            m_jSubtotalEuros.setText(m_oTicket.printSubTotal());
            m_jTaxesEuros.setText(m_oTicket.printTax());
            m_jTotalEuros.setText(m_oTicket.printTotal());
        }
    }
    
    private void paintTicketLine(int index, TicketLineInfo oLine){
        
        if (executeEventAndRefresh("ticket.setline", new ScriptArg("index", index), new ScriptArg("line", oLine)) == null) {

            m_oTicket.setLine(index, oLine);
            m_ticketlines.setTicketLine(index, oLine);
            m_ticketlines.setSelectedIndex(index);

            visorTicketLine(oLine); // Y al visor tambien...
            printPartialTotals();   
            stateToZero();  

            // event receipt
            executeEventAndRefresh("ticket.change");
        }
   }

    private void addTicketLine(ProductInfoExt oProduct, double dMul, double dPrice) {   
        /*for(int i=0; i<m_oTicket.getLinesCount();i++)
        {
            if(m_oTicket.getLine(i).getProductID().equalsIgnoreCase(oProduct.getID()))
            {
                JOptionPane.showMessageDialog(this, "El producto "+oProduct.getName()+" ya ha sido cargado en este ticket.");
                refreshTicket();
                return;
            }
        }*/
        /*try {
            
            double units = dlSales.findProductStock("0",oProduct.getID(),null);
            if(units>0&&m_oTicket.getTicketType()==TicketInfo.RECEIPT_NORMAL){*/
                TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(),  m_oTicket.getDate(), m_oTicket.getCustomer());
                addTicketLine(new TicketLineInfo(oProduct, dMul, dPrice, tax, (java.util.Properties) (oProduct.getProperties().clone())));
            /*} else
            {
                JOptionPane.showMessageDialog(this, "El producto "+oProduct.getName()+" NO cuenta con inventario capturado. Inventario actual: "+String.valueOf(units));
                refreshTicket();
            }
        } catch (BasicException ex) {
            Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }
    
    protected void addTicketLine(TicketLineInfo oLine) {   
        
        
                m_oTicket.addLine(oLine);            
                m_ticketlines.addTicketLine(oLine); // Pintamos la linea en la vista... 
                
                
            visorTicketLine(oLine);
            printPartialTotals();
            stateToZero();  

            // event receipt
            executeEventAndRefresh("ticket.change");
            refreshTicket();
        
    }    
    
    private void removeTicketLine(int i){
        
        if (executeEventAndRefresh("ticket.removeline", new ScriptArg("index", i)) == null) {
        
            if (m_oTicket.getLine(i).isProductCom()) {
                // Es un producto auxiliar, lo borro y santas pascuas.
                m_oTicket.removeLine(i);
                m_ticketlines.removeTicketLine(i);   
            } else {
                // Es un producto normal, lo borro.
                m_oTicket.removeLine(i);
                m_ticketlines.removeTicketLine(i); 
                // Y todos lo auxiliaries que hubiera debajo.
                while(i < m_oTicket.getLinesCount() && m_oTicket.getLine(i).isProductCom()) {
                    m_oTicket.removeLine(i);
                    m_ticketlines.removeTicketLine(i); 
                }
            }

            visorTicketLine(null); // borro el visor 
            printPartialTotals(); // pinto los totales parciales...                           
            stateToZero(); // Pongo a cero    

            // event receipt
            executeEventAndRefresh("ticket.change");
        }
    }
    
    private ProductInfoExt getInputProduct() {
        ProductInfoExt oProduct = new ProductInfoExt(); // Es un ticket
        oProduct.setReference(null);
        oProduct.setCode(null);
        oProduct.setName("");
        oProduct.setTaxCategoryID(((TaxCategoryInfo) taxcategoriesmodel.getSelectedItem()).getID());
        
        oProduct.setPriceSell(includeTaxes(oProduct.getTaxCategoryID(), getInputValue()));
        
        return oProduct;
    }
    
    private double includeTaxes(String tcid, double dValue) {
        if (m_jaddtax.isSelected()) {
            TaxInfo tax = taxeslogic.getTaxInfo(tcid,  m_oTicket.getDate(), m_oTicket.getCustomer());
            double dTaxRate = tax == null ? 0.0 : tax.getRate();           
            return dValue / (1.0 + dTaxRate);      
        } else {
            return dValue;
        }
    }
    
    private double getInputValue() {
        try {
            return Double.parseDouble(m_jPrice.getText());
        } catch (NumberFormatException e){
            return 0.0;
        }
    }

    private double getPorValue() {
        try {
            return Double.parseDouble(m_jPor.getText().substring(1));                
        } catch (NumberFormatException e){
            return 1.0;
        } catch (StringIndexOutOfBoundsException e){
            return 1.0;
        }
    }
    
    private void stateToZero(){
        m_jPor.setText("");
        m_jPrice.setText("");
        m_sBarcode = new StringBuffer();

        m_iNumberStatus = NUMBER_INPUTZERO;
        m_iNumberStatusInput = NUMBERZERO;
        m_iNumberStatusPor = NUMBERZERO;
    }
    
    private void incProductByCode(String sCode) {
    // precondicion: sCode != null
        
        try {
            ProductInfoExt oProduct = dlSales.getProductInfoByCode(sCode);
            if (oProduct == null) {                  
                Toolkit.getDefaultToolkit().beep();              
                new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noproduct")).show(this);           
                stateToZero();
            } else {
                // Se anade directamente una unidad con el precio y todo
                incProduct(oProduct);
            }
        } catch (BasicException eData) {
            stateToZero();           
            new MessageInf(eData).show(this);           
        }
    }
    
    private void incProductByCodePrice(String sCode, double dPriceSell) {
    // precondicion: sCode != null
        
        try {
            ProductInfoExt oProduct = dlSales.getProductInfoByCode(sCode);
            if (oProduct == null) {                  
                Toolkit.getDefaultToolkit().beep();                   
                new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noproduct")).show(this);           
                stateToZero();
            } else {
                // Se anade directamente una unidad con el precio y todo
                if (m_jaddtax.isSelected()) {
                    // debemos quitarle los impuestos ya que el precio es con iva incluido...
                    TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(),  m_oTicket.getDate(), m_oTicket.getCustomer());
                    addTicketLine(oProduct, 1.0, dPriceSell / (1.0 + tax.getRate()));
                } else {
                    addTicketLine(oProduct, 1.0, dPriceSell);
                }                
            }
        } catch (BasicException eData) {
            stateToZero();
            new MessageInf(eData).show(this);               
        }
    }
    private void incProductByCodePrice2(String sCode, double dPriceSell, double mul) {
    // precondicion: sCode != null
        
        try {
            ProductInfoExt oProduct = dlSales.getProductInfoByCode(sCode);
            if (oProduct == null) {                  
                Toolkit.getDefaultToolkit().beep();                   
                new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noproduct")).show(this);           
                stateToZero();
            } else {
                // Se anade directamente una unidad con el precio y todo
                if (m_jaddtax.isSelected()) {
                    // debemos quitarle los impuestos ya que el precio es con iva incluido...
                    TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(),  m_oTicket.getDate(), m_oTicket.getCustomer());
                    addTicketLine(oProduct, mul, dPriceSell / (1.0 + tax.getRate()));
                } else {
                    addTicketLine(oProduct, mul, dPriceSell);
                }                
            }
        } catch (BasicException eData) {
            stateToZero();
            new MessageInf(eData).show(this);               
        }
    }
    
    private void incProduct(ProductInfoExt prod) {
        
        if (prod.isScale() && m_App.getDeviceScale().existsScale()) {
            try {
                Double value = m_App.getDeviceScale().readWeight();
                if (value != null) {
                    incProduct(value.doubleValue(), prod);
                }
            } catch (ScaleException e) {
                Toolkit.getDefaultToolkit().beep();                
                new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);           
                stateToZero(); 
            }
        } else {
            // No es un producto que se pese o no hay balanza
            incProduct(1.0, prod);
        }
    }
    
    private void incProduct(double dPor, ProductInfoExt prod) {
        // precondicion: prod != null
        addTicketLine(prod, dPor, prod.getPriceSell());       
    }
       
    protected void buttonTransition(ProductInfoExt prod) {
    // precondicion: prod != null
        
         if (m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
            incProduct(prod);
        } else if (m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
            incProduct(getInputValue(), prod);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }       
    }
    
    private void stateTransition(char cTrans) {

        if (cTrans == '\n') {
            // Codigo de barras introducido
            if (m_sBarcode.length() > 0) {            
                String sCode = m_sBarcode.toString();
                if(sCode.length()==9) sCode = "0"+sCode;
                if (sCode.length()==10) {
                    BigInteger num = new BigInteger(sCode, 16);
                    String binary = String.format("%40s", num.toString(2)).replace(' ','0');
                    System.out.println(binary);
                    try {
                    int x = Integer.parseInt(binary.substring(9, 17), 2);
                    int y = Integer.parseInt(binary.substring(17, 33), 2);
                    System.out.println(x);
                    System.out.println(y);
                    String z;
                    if(y<=9) z = "0000"+String.valueOf(y);
                    else if(y<=99) z = "000"+String.valueOf(y);
                    else if(y<=999) z = "00"+String.valueOf(y);
                    else if(y<=9999) z = "0"+String.valueOf(y);
                    else z=String.valueOf(y);
                        CustomerInfoExt newcustomer = dlSales.findCustomerExt(""+x+z);
                        if (newcustomer == null) {
                            //Toolkit.getDefaultToolkit().beep();                   
                            //JOptionPane.showMessageDialog(this, "No se encuentra el empleado o está inactivo");
                            new MessageInf(MessageInf.SGN_WARNING, "No se encuentra el empleado o está inactivo").show(this);           
                        } else {
                            m_oTicket.setCustomer(newcustomer);
                            m_jTicketId.setText(m_oTicket.getName(m_oTicketExt));
                        }
                    } catch (BasicException e) {
                        Toolkit.getDefaultToolkit().beep();                   
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.nocustomer"), e).show(this);           
                    }
                    stateToZero();
                /*} else if (sCode.length() == 13 && sCode.startsWith("250")) {
                    // barcode of the other machine
                    ProductInfoExt oProduct = new ProductInfoExt(); // Es un ticket
                    oProduct.setReference(null); // para que no se grabe
                    oProduct.setCode(sCode);
                    oProduct.setName("Ticket " + sCode.substring(3, 7));
                    oProduct.setPriceSell(Double.parseDouble(sCode.substring(7, 12)) / 100);   
                    oProduct.setTaxCategoryID(((TaxCategoryInfo) taxcategoriesmodel.getSelectedItem()).getID());
                    // Se anade directamente una unidad con el precio y todo
                    addTicketLine(oProduct, 1.0, includeTaxes(oProduct.getTaxCategoryID(), oProduct.getPriceSell()));
                } else if (sCode.length() == 13 && sCode.startsWith("210")) {
                    // barcode of a weigth product
                    incProductByCodePrice(sCode.substring(0, 7), Double.parseDouble(sCode.substring(7, 12)) / 100);
                } 
                else if (sCode.length() == 13 && sCode.startsWith("0200")) {
                    //int cod = Integer.parseInt(sCode.substring(1, 7));
                    String codigo = "0"; //Integer.toString(cod);
                    double precio = Double.parseDouble(sCode.substring(7, 12))/100;
                    //double mul = Double.parseDouble(sCode.substring(7, 12))/100;
                    try {
                        ProductInfoExt oProduct = dlSales.getProductInfoByCode(codigo);
                        //mul = precio / oProduct.getPriceSell();
                            
                                addTicketLine(oProduct, 1.0, precio);      
                    } catch (BasicException eData) {
                        stateToZero();
                        new MessageInf(eData).show(this);               
                    }
                    closeTicket(m_oTicket, m_oTicketExt);*/
                }
                else {
                    incProductByCode(sCode);
                }
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            // otro caracter
            // Esto es para el codigo de barras...
            m_sBarcode.append(cTrans);

            // Esto es para el los productos normales...
            if (cTrans == '\u007f') { 
                stateToZero();

            } else if ((cTrans == '0' ||cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_INPUTZERO)) { 
                // Un numero entero
                m_jPrice.setText(Character.toString(cTrans));
                m_iNumberStatus = NUMBER_INPUTINT;    
                m_iNumberStatusInput = NUMBERVALID;
            }
            
            else if ((cTrans == '0' || cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                       && (m_iNumberStatus == NUMBER_INPUTINT)) { 
                // Un numero entero
                m_jPrice.setText(m_jPrice.getText() + cTrans);

            } else if (cTrans == '.' && m_iNumberStatus == NUMBER_INPUTZERO) {
                m_jPrice.setText("0.");
                m_iNumberStatus = NUMBER_INPUTZERODEC;            
            } else if (cTrans == '.' && m_iNumberStatus == NUMBER_INPUTINT) {
                m_jPrice.setText(m_jPrice.getText() + ".");
                m_iNumberStatus = NUMBER_INPUTDEC;

            } else if ((cTrans == '0')
                       && (m_iNumberStatus == NUMBER_INPUTZERODEC || m_iNumberStatus == NUMBER_INPUTDEC)) { 
                // Un numero decimal
                m_jPrice.setText(m_jPrice.getText() + cTrans);
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                       && (m_iNumberStatus == NUMBER_INPUTZERODEC || m_iNumberStatus == NUMBER_INPUTDEC)) { 
                // Un numero decimal
                m_jPrice.setText(m_jPrice.getText() + cTrans);
                m_iNumberStatus = NUMBER_INPUTDEC;
                m_iNumberStatusInput = NUMBERVALID;

            } else if (cTrans == '*' 
                    && (m_iNumberStatus == NUMBER_INPUTINT || m_iNumberStatus == NUMBER_INPUTDEC)) {
                m_jPor.setText("x");
                m_iNumberStatus = NUMBER_PORZERO;            
            } else if (cTrans == '*' 
                    && (m_iNumberStatus == NUMBER_INPUTZERO || m_iNumberStatus == NUMBER_INPUTZERODEC)) {
                m_jPrice.setText("0");
                m_jPor.setText("x");
                m_iNumberStatus = NUMBER_PORZERO;       

            } else if ((cTrans == '0') 
                    && (m_iNumberStatus == NUMBER_PORZERO)) {
                m_jPor.setText("x0");            
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_PORZERO)) { 
                // Un numero entero
                m_jPor.setText("x" + Character.toString(cTrans));
                m_iNumberStatus = NUMBER_PORINT;            
                m_iNumberStatusPor = NUMBERVALID;
            } else if ((cTrans == '0' || cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                       && (m_iNumberStatus == NUMBER_PORINT)) { 
                // Un numero entero
                m_jPor.setText(m_jPor.getText() + cTrans);

            } else if (cTrans == '.' && m_iNumberStatus == NUMBER_PORZERO) {
                m_jPor.setText("x0.");
                m_iNumberStatus = NUMBER_PORZERODEC;            
            } else if (cTrans == '.' && m_iNumberStatus == NUMBER_PORINT) {
                m_jPor.setText(m_jPor.getText() + ".");
                m_iNumberStatus = NUMBER_PORDEC;

            } else if ((cTrans == '0')
                       && (m_iNumberStatus == NUMBER_PORZERODEC || m_iNumberStatus == NUMBER_PORDEC)) { 
                // Un numero decimal
                m_jPor.setText(m_jPor.getText() + cTrans);
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                       && (m_iNumberStatus == NUMBER_PORZERODEC || m_iNumberStatus == NUMBER_PORDEC)) { 
                // Un numero decimal
                m_jPor.setText(m_jPor.getText() + cTrans);
                m_iNumberStatus = NUMBER_PORDEC;            
                m_iNumberStatusPor = NUMBERVALID;  
            
            } else if (cTrans == '\u00a7' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
                // Scale button pressed and a number typed as a price
                if (m_App.getDeviceScale().existsScale() && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                    try {
                        ProductInfoExt product = getInputProduct();
                        ScaleDialog s = new ScaleDialog(this, product);
                        Double value = s.readWeight();
                        if (value != null) {
                            //ProductInfoExt product = getInputProduct();
                            addTicketLine(product, value.doubleValue(), product.getPriceSell());
                            
                        }
                    } catch (ScaleException e) {
                        Toolkit.getDefaultToolkit().beep();
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);           
                        stateToZero(); 
                    }
                } else {
                    // No existe la balanza;
                    Toolkit.getDefaultToolkit().beep();
                }
            } else if (cTrans == '\u00a7' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
                // Scale button pressed and no number typed.
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else if (m_App.getDeviceScale().existsScale()) {
                    try {
                        try
                        {
                        TicketLineInfo newline = new TicketLineInfo(this.m_oTicket.getLine(i));
                        String productID = newline.getProductID();
                        ProductInfoExt productInfo = this.dlSales.getProductInfo(productID);
                        ScaleDialog s = new ScaleDialog(this, productInfo);
                        Double value = m_App.getDeviceScale().readWeight();
                        if (value != null) {
                            //TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                            newline.setMultiply(value.doubleValue());
                            newline.setPrice(newline.getPrice());
                            paintTicketLine(i, newline);
                        }
                        }catch (BasicException ex)
                        {
                          Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } catch (ScaleException e) {
                        // Error de pesada.
                        Toolkit.getDefaultToolkit().beep();
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);           
                        stateToZero(); 
                    }
                } else {
                    // No existe la balanza;
                    Toolkit.getDefaultToolkit().beep();
                }      
                
            // Add one product more to the selected line
            } else if (cTrans == '+' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                    //If it's a refund + button means one unit less
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND){
                        newline.setMultiply(newline.getMultiply() - 1.0);
                        paintTicketLine(i, newline);                   
                    }
                    else {
                        // add one unit to the selected line
                        newline.setMultiply(newline.getMultiply() + 1.0);
                        paintTicketLine(i, newline); 
                    }
                }

            // Delete one product of the selected line
            } else if (cTrans == '-' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                    //If it's a refund - button means one unit more
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND){
                        newline.setMultiply(newline.getMultiply() + 1.0);
                        if (newline.getMultiply() >= 0) {
                            removeTicketLine(i);
                        } else {
                            paintTicketLine(i, newline);
                        }
                    } else {
                        // substract one unit to the selected line
                        newline.setMultiply(newline.getMultiply() - 1.0);
                        if (newline.getMultiply() <= 0.0) {                   
                            removeTicketLine(i); // elimino la linea
                        } else {
                            paintTicketLine(i, newline);                   
                        }
                    }
                }

            // Set n products to the selected line
            } else if (cTrans == '+' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERVALID) {
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    double dPor = getPorValue();
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i)); 
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                        newline.setMultiply(-dPor);
                        newline.setPrice(newline.getPrice());
                        paintTicketLine(i, newline);                
                    } else {
                        newline.setMultiply(dPor);
                        newline.setPrice(newline.getPrice());
                        paintTicketLine(i, newline);
                    }
                }

            // Set n negative products to the selected line
            } else if (cTrans == '-' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERVALID
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    double dPor = getPorValue();
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_NORMAL) {
                        newline.setMultiply(dPor);
                        newline.setPrice(-newline.getPrice());
                        paintTicketLine(i, newline);
                    }           
                }

            // Anadimos 1 producto
            } else if (cTrans == '+' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, 1.0, product.getPriceSell());
                
            // Anadimos 1 producto con precio negativo
            } else if (cTrans == '-' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, 1.0, -product.getPriceSell());

            // Anadimos n productos
            } else if (cTrans == '+' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERVALID
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, getPorValue(), product.getPriceSell());

            // Anadimos n productos con precio negativo ?
            } else if (cTrans == '-' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERVALID
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, getPorValue(), -product.getPriceSell());

            // Totals() Igual;
            } else if (cTrans == ' ' || cTrans == '=') {
                if (m_oTicket.getLinesCount() > 0) {
                    
                    if (closeTicket(m_oTicket, m_oTicketExt)) {
                        // Ends edition of current receipt
                        m_ticketsbag.deleteTicket();  
                    } else {
                        // repaint current ticket
                        refreshTicket();
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
            else
            {
                m_jPrice.setText(m_jPrice.getText() + cTrans);
            }
        }
    }
    
    private boolean closeTicket(TicketInfo ticket, Object ticketext) {
    /*if(ticket.getCustomer()==null)
    {
        return false;
    }*/
        boolean resultok = false;
        /*for(int i=0; i<m_oTicket.getLinesCount();i++)
        {
            try {
                double units = dlSales.findProductStock("000",m_oTicket.getLine(i).getProductID(),null);
                if(units<m_oTicket.getLine(i).getMultiply())
                {
                    JOptionPane.showMessageDialog(this, "NO puedes vender "+m_oTicket.getLine(i).printMultiply()+" unidades del producto "+ m_oTicket.getLine(i).printName() +" ya que solo cuentas con "+String.valueOf(units)+ " en inventario.");
                    refreshTicket();
                    return false;
                }
            } catch (BasicException ex) {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }*/
        if (m_App.getAppUserView().getUser().hasPermission("sales.Total")) {  
            
            try {
                // reset the payment info
                taxeslogic.calculateTaxes(ticket);
                if (ticket.getTotal()>=0.0){
                    ticket.resetPayments(); //Only reset if is sale
                }
                
                if (executeEvent(ticket, ticketext, "ticket.total") == null) {

                    // Muestro el total
                    printTicket("Printer.TicketTotal", ticket, ticketext);
                    
                    // Select the Payments information
                    JPaymentSelect paymentdialog = ticket.getTicketType() == TicketInfo.RECEIPT_NORMAL
                            ? paymentdialogreceipt
                            : paymentdialogrefund;
                    paymentdialog.setPrintSelected("true".equals(m_jbtnconfig.getProperty("printselected", "true")));

                    paymentdialog.setTransactionID(ticket.getTransactionID());

                    if (paymentdialog.showDialog(ticket.getTotal(), ticket.getCustomer(), false)) {
                        ticket.setPayments(paymentdialog.getSelectedPayments());

                        // Asigno los valores definitivos del ticket...
                        ticket.setUser(m_App.getAppUserView().getUser().getUserInfo()); // El usuario que lo cobra
                        ticket.setActiveCash(m_App.getActiveCashIndex());
                        ticket.setDate(new Date()); // Le pongo la fecha de cobro
                        //ticket.setProperty("sequence", String.valueOf(m_App.getActiveCashSequence()));
                        if (executeEvent(ticket, ticketext, "ticket.save") == null) {
                            try {
                                dlSales.saveTicket(ticket, m_App.getInventoryLocation());                       
                            } catch (BasicException eData) {
                                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.nosaveticket"), eData);
                                msg.show(this);
                            }

                            executeEvent(ticket, ticketext, "ticket.close", new ScriptArg("print", paymentdialog.isPrintSelected()));

                            // Print receipt.
                            if(paymentdialog.isPrintSelected())
                            {
                                printTicket("Printer.Ticket", ticket, ticketext);
                                if(m_App.getAppUserView().getUser().hasPermission("dobleTicket"))
                                {
                                    JOptionPane.showMessageDialog(this, "Corte el ticket y haga clic en aceptar.");
                                    printTicket("Printer.Ticket", ticket, ticketext);
                                }
                            }
                            resultok = true;
                        }
                    }
                }
            } catch (TaxesException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotcalculatetaxes"));
                msg.show(this);
                resultok = false;
            }
            
            // reset the payment info
            m_oTicket.resetTaxes();
            m_oTicket.resetPayments();
        }
        
        // cancelled the ticket.total script
        // or canceled the payment dialog
        // or canceled the ticket.close script
        return resultok;        
    }
    private boolean closeTicket2(TicketInfo ticket, Object ticketext) {
        
        boolean resultok = false;
        
                java.util.List<PaymentInfo> p = new ArrayList<PaymentInfo>();
                p.add(new PaymentInfoFree(ticket.getTotal()));
                        ticket.setPayments(p);

                        // Asigno los valores definitivos del ticket...
                        ticket.setUser(m_App.getAppUserView().getUser().getUserInfo()); // El usuario que lo cobra
                        ticket.setActiveCash(m_App.getActiveCashIndex());
                        ticket.setDate(new Date()); // Le pongo la fecha de cobro
                        //ticket.setProperty("sequence", String.valueOf(m_App.getActiveCashSequence()));
                            try {
                                dlSales.saveTicket3(ticket, m_App.getInventoryLocation());                       
                            } catch (BasicException eData) {
                                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.nosaveticket"), eData);
                                msg.show(this);
                            }

                            
                                printTicket("Printer.Ticket", ticket, ticketext);
                                if(m_App.getAppUserView().getUser().hasPermission("dobleTicket"))
                                {
                                    JOptionPane.showMessageDialog(this, "Corte el ticket y haga clic en aceptar.");
                                    printTicket("Printer.Ticket", ticket, ticketext);
                                }
                            
                            resultok = true;
                        

            
            // reset the payment info
            m_oTicket.resetTaxes();
            m_oTicket.resetPayments();
        return resultok;        
    }
    private void printTicket(String sresourcename, TicketInfo ticket, Object ticketext) {

        String sresource = dlSystem.getResourceAsXML(sresourcename);
        if (sresource == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"));
            msg.show(JPanelTicket.this);
        } else {
            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                script.put("taxes", taxcollection);
                script.put("taxeslogic", taxeslogic);
                script.put("ticket", ticket);
                script.put("place", ticketext);
                m_TTP.printTicket(script.eval(sresource).toString());
            } catch (ScriptException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"), e);
                msg.show(JPanelTicket.this);
            } catch (TicketPrinterException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"), e);
                msg.show(JPanelTicket.this);
            }
        }
    }
    
    private void printReport(String resourcefile, TicketInfo ticket, Object ticketext) {
        
        try {     
         
            JasperReport jr;
           
            InputStream in = getClass().getResourceAsStream(resourcefile + ".ser");
            if (in == null) {      
                // read and compile the report
                JasperDesign jd = JRXmlLoader.load(getClass().getResourceAsStream(resourcefile + ".jrxml"));            
                jr = JasperCompileManager.compileReport(jd);    
            } else {
                // read the compiled reporte
                ObjectInputStream oin = new ObjectInputStream(in);
                jr = (JasperReport) oin.readObject();
                oin.close();
            }
           
            // Construyo el mapa de los parametros.
            Map reportparams = new HashMap();
            // reportparams.put("ARG", params);
            try {
                reportparams.put("REPORT_RESOURCE_BUNDLE", ResourceBundle.getBundle(resourcefile + ".properties"));
            } catch (MissingResourceException e) {
            }
            reportparams.put("TAXESLOGIC", taxeslogic); 
            
            Map reportfields = new HashMap();
            reportfields.put("TICKET", ticket);
            reportfields.put("PLACE", ticketext);

            JasperPrint jp = JasperFillManager.fillReport(jr, reportparams, new JRMapArrayDataSource(new Object[] { reportfields } ));
            
            PrintService service = ReportUtils.getPrintService(m_App.getProperties().getProperty("machine.printername"));
            
            JRPrinterAWT300.printPages(jp, 0, jp.getPages().size() - 1, service);
            
        } catch (Exception e) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e);
            msg.show(this);
        }               
    }

    private void visorTicketLine(TicketLineInfo oLine){
        if (oLine == null) { 
             m_App.getDeviceTicket().getDeviceDisplay().clearVisor();
        } else {                 
            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                script.put("ticketline", oLine);
                m_TTP.printTicket(script.eval(dlSystem.getResourceAsXML("Printer.TicketLine")).toString());
            } catch (ScriptException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintline"), e);
                msg.show(JPanelTicket.this);
            } catch (TicketPrinterException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintline"), e);
                msg.show(JPanelTicket.this);
            }
        } 
    }    
    
    
    private Object evalScript(ScriptObject scr, String resource, ScriptArg... args) {
        
        // resource here is guaratied to be not null
         try {
            scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
            return scr.evalScript(dlSystem.getResourceAsXML(resource), args);                
        } catch (ScriptException e) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"), e);
            msg.show(this);
            return msg;
        } 
    }
        
    public void evalScriptAndRefresh(String resource, ScriptArg... args) {

        if (resource == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"));
            msg.show(this);            
        } else {
            ScriptObject scr = new ScriptObject(m_oTicket, m_oTicketExt);
            scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
            evalScript(scr, resource, args);   
            refreshTicket();
            setSelectedIndex(scr.getSelectedIndex());
        }
    }  
    
    public void printTicket(String resource) {
        printTicket(resource, m_oTicket, m_oTicketExt);
    }
    
    private Object executeEventAndRefresh(String eventkey, ScriptArg ... args) {
        
        String resource = m_jbtnconfig.getEvent(eventkey);
        if (resource == null) {
            return null;
        } else {
            ScriptObject scr = new ScriptObject(m_oTicket, m_oTicketExt);
            scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
            Object result = evalScript(scr, resource, args);   
            refreshTicket();
            setSelectedIndex(scr.getSelectedIndex());
            return result;
        }
    }
   
    private Object executeEvent(TicketInfo ticket, Object ticketext, String eventkey, ScriptArg ... args) {
        
        String resource = m_jbtnconfig.getEvent(eventkey);
        if (resource == null) {
            return null;
        } else {
            ScriptObject scr = new ScriptObject(ticket, ticketext);
            return evalScript(scr, resource, args);
        }
    }
    
    public String getResourceAsXML(String sresourcename) {
        return dlSystem.getResourceAsXML(sresourcename);
    }

    public BufferedImage getResourceAsImage(String sresourcename) {
        return dlSystem.getResourceAsImage(sresourcename);
    }
    
    private void setSelectedIndex(int i) {
        
        if (i >= 0 && i < m_oTicket.getLinesCount()) {
            m_ticketlines.setSelectedIndex(i);
        } else if (m_oTicket.getLinesCount() > 0) {
            m_ticketlines.setSelectedIndex(m_oTicket.getLinesCount() - 1);
        }    
    }
     
    public static class ScriptArg {
        private String key;
        private Object value;
        
        public ScriptArg(String key, Object value) {
            this.key = key;
            this.value = value;
        }
        public String getKey() {
            return key;
        }
        public Object getValue() {
            return value;
        }
    }
    
    public class ScriptObject {
        
        private TicketInfo ticket;
        private Object ticketext;
        
        private int selectedindex;
        
        private ScriptObject(TicketInfo ticket, Object ticketext) {
            this.ticket = ticket;
            this.ticketext = ticketext;
        }
        
        public double getInputValue() {
            if (m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
                return JPanelTicket.this.getInputValue();
            } else {
                return 0.0;
            }
        }
        
        public int getSelectedIndex() {
            return selectedindex;
        }
        
        public void setSelectedIndex(int i) {
            selectedindex = i;
        }  
        
        public void printReport(String resourcefile) {
            JPanelTicket.this.printReport(resourcefile, ticket, ticketext);
        }
        
        public void printTicket(String sresourcename) {
            JPanelTicket.this.printTicket(sresourcename, ticket, ticketext);   
        }              
        
        public Object evalScript(String code, ScriptArg... args) throws ScriptException {
            
            ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.BEANSHELL);
            script.put("ticket", ticket);
            script.put("place", ticketext);
            script.put("taxes", taxcollection);
            script.put("taxeslogic", taxeslogic);             
            script.put("user", m_App.getAppUserView().getUser());
            script.put("sales", this);

            // more arguments
            for(ScriptArg arg : args) {
                script.put(arg.getKey(), arg.getValue());
            }             

            return script.eval(code);
        }            
    }
     
/** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the FormEditor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        m_jNumberKeys = new com.openbravo.beans.JNumberKeys();
        jEditAttributes = new javax.swing.JButton();
        m_jPor = new javax.swing.JLabel();
        m_jTax = new javax.swing.JComboBox();
        m_jaddtax = new javax.swing.JToggleButton();
        jPanel5 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        m_jUp = new javax.swing.JButton();
        m_jDown = new javax.swing.JButton();
        jEditAttributes1 = new javax.swing.JButton();
        m_jbtnScale = new javax.swing.JButton();
        m_jEditLine1 = new javax.swing.JButton();
        btnCustomer = new javax.swing.JButton();
        btnCustomer3 = new javax.swing.JButton();
        btnSplit2 = new javax.swing.JButton();
        m_jTaxesEuros = new javax.swing.JLabel();
        m_jLblTotalEuros2 = new javax.swing.JLabel();
        m_jSubtotalEuros = new javax.swing.JLabel();
        m_jLblTotalEuros3 = new javax.swing.JLabel();
        m_jEnter = new javax.swing.JButton();
        btnCustomer1 = new javax.swing.JButton();
        btnSplit1 = new javax.swing.JButton();
        btnSplit = new javax.swing.JButton();
        btnCustomer4 = new javax.swing.JButton();
        m_jPanContainer = new javax.swing.JPanel();
        m_jOptions = new javax.swing.JPanel();
        m_jButtons = new javax.swing.JPanel();
        m_jTicketId = new javax.swing.JLabel();
        m_jDelete = new javax.swing.JButton();
        m_jList = new javax.swing.JButton();
        m_jEditLine = new javax.swing.JButton();
        jEditAttributes2 = new javax.swing.JButton();
        jEditAttributes3 = new javax.swing.JButton();
        btnCustomer2 = new javax.swing.JButton();
        m_jPanelScripts = new javax.swing.JPanel();
        m_jButtonsExt = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        m_jbtnScale1 = new javax.swing.JButton();
        m_jPanelBag = new javax.swing.JPanel();
        m_jContEntries = new javax.swing.JPanel();
        m_jPanEntries = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        m_jPrice = new javax.swing.JLabel();
        m_jKeyFactory = new javax.swing.JTextField();
        m_jPanTicket = new javax.swing.JPanel();
        m_jPanelCentral = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        m_jPanTotals = new javax.swing.JPanel();
        m_jTotalEuros = new javax.swing.JLabel();
        m_jLblTotalEuros1 = new javax.swing.JLabel();
        m_jContEntries1 = new javax.swing.JPanel();
        m_jPanEntries1 = new javax.swing.JPanel();
        m_jNumberKeys1 = new com.openbravo.beans.JNumberKeys();
        jPanel10 = new javax.swing.JPanel();
        m_jKeyFactory1 = new javax.swing.JTextField();
        catcontainer = new javax.swing.JPanel();

        m_jNumberKeys.addJNumberEventListener(new com.openbravo.beans.JNumberEventListener() {
            public void keyPerformed(com.openbravo.beans.JNumberEvent evt) {
                m_jNumberKeysKeyPerformed(evt);
            }
        });

        jEditAttributes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/colorize.png"))); // NOI18N
        jEditAttributes.setFocusPainted(false);
        jEditAttributes.setFocusable(false);
        jEditAttributes.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jEditAttributes.setRequestFocusEnabled(false);
        jEditAttributes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributesActionPerformed(evt);
            }
        });

        m_jPor.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jPor.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jPor.setOpaque(true);
        m_jPor.setPreferredSize(new java.awt.Dimension(22, 22));
        m_jPor.setRequestFocusEnabled(false);

        m_jTax.setFocusable(false);
        m_jTax.setRequestFocusEnabled(false);

        m_jaddtax.setText("+");
        m_jaddtax.setFocusPainted(false);
        m_jaddtax.setFocusable(false);
        m_jaddtax.setRequestFocusEnabled(false);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        jPanel2.setLayout(new java.awt.GridLayout(0, 1, 5, 5));

        m_jUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/1uparrow22.png"))); // NOI18N
        m_jUp.setFocusPainted(false);
        m_jUp.setFocusable(false);
        m_jUp.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jUp.setRequestFocusEnabled(false);
        m_jUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jUpActionPerformed(evt);
            }
        });
        jPanel2.add(m_jUp);

        m_jDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/1downarrow22.png"))); // NOI18N
        m_jDown.setFocusPainted(false);
        m_jDown.setFocusable(false);
        m_jDown.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jDown.setRequestFocusEnabled(false);
        m_jDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jDownActionPerformed(evt);
            }
        });
        jPanel2.add(m_jDown);

        jPanel5.add(jPanel2, java.awt.BorderLayout.NORTH);

        jEditAttributes1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/edit_group.png"))); // NOI18N
        jEditAttributes1.setFocusPainted(false);
        jEditAttributes1.setFocusable(false);
        jEditAttributes1.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jEditAttributes1.setPreferredSize(new java.awt.Dimension(73, 62));
        jEditAttributes1.setRequestFocusEnabled(false);
        jEditAttributes1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributes1ActionPerformed(evt);
            }
        });

        m_jbtnScale.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/pay.png"))); // NOI18N
        m_jbtnScale.setFocusPainted(false);
        m_jbtnScale.setFocusable(false);
        m_jbtnScale.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jbtnScale.setPreferredSize(new java.awt.Dimension(77, 56));
        m_jbtnScale.setRequestFocusEnabled(false);
        m_jbtnScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtnScaleActionPerformed(evt);
            }
        });

        m_jEditLine1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/button_ok.png"))); // NOI18N
        m_jEditLine1.setFocusPainted(false);
        m_jEditLine1.setFocusable(false);
        m_jEditLine1.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jEditLine1.setRequestFocusEnabled(false);
        m_jEditLine1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEditLine1ActionPerformed(evt);
            }
        });

        btnCustomer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/kuser.png"))); // NOI18N
        btnCustomer.setFocusPainted(false);
        btnCustomer.setFocusable(false);
        btnCustomer.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer.setPreferredSize(new java.awt.Dimension(73, 62));
        btnCustomer.setRequestFocusEnabled(false);
        btnCustomer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomerActionPerformed(evt);
            }
        });

        btnCustomer3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/button_ok.png"))); // NOI18N
        btnCustomer3.setFocusPainted(false);
        btnCustomer3.setFocusable(false);
        btnCustomer3.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer3.setPreferredSize(new java.awt.Dimension(63, 62));
        btnCustomer3.setRequestFocusEnabled(false);
        btnCustomer3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer3ActionPerformed(evt);
            }
        });

        btnSplit2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/refund.png"))); // NOI18N
        btnSplit2.setFocusPainted(false);
        btnSplit2.setFocusable(false);
        btnSplit2.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnSplit2.setPreferredSize(new java.awt.Dimension(63, 62));
        btnSplit2.setRequestFocusEnabled(false);
        btnSplit2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSplit2ActionPerformed(evt);
            }
        });

        m_jTaxesEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        m_jTaxesEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jTaxesEuros.setOpaque(true);
        m_jTaxesEuros.setPreferredSize(new java.awt.Dimension(150, 25));
        m_jTaxesEuros.setRequestFocusEnabled(false);

        m_jLblTotalEuros2.setText(AppLocal.getIntString("label.taxcash")); // NOI18N

        m_jSubtotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        m_jSubtotalEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jSubtotalEuros.setOpaque(true);
        m_jSubtotalEuros.setPreferredSize(new java.awt.Dimension(150, 25));
        m_jSubtotalEuros.setRequestFocusEnabled(false);

        m_jLblTotalEuros3.setText(AppLocal.getIntString("label.subtotalcash")); // NOI18N

        m_jEnter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/barcode.png"))); // NOI18N
        m_jEnter.setFocusPainted(false);
        m_jEnter.setFocusable(false);
        m_jEnter.setPreferredSize(new java.awt.Dimension(34, 30));
        m_jEnter.setRequestFocusEnabled(false);
        m_jEnter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEnterActionPerformed(evt);
            }
        });

        btnCustomer1.setText("%");
        btnCustomer1.setFocusPainted(false);
        btnCustomer1.setFocusable(false);
        btnCustomer1.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer1.setMaximumSize(new java.awt.Dimension(54, 42));
        btnCustomer1.setMinimumSize(new java.awt.Dimension(54, 42));
        btnCustomer1.setPreferredSize(new java.awt.Dimension(63, 62));
        btnCustomer1.setRequestFocusEnabled(false);
        btnCustomer1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer1ActionPerformed(evt);
            }
        });

        btnSplit1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/pay.png"))); // NOI18N
        btnSplit1.setFocusPainted(false);
        btnSplit1.setFocusable(false);
        btnSplit1.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnSplit1.setPreferredSize(new java.awt.Dimension(63, 62));
        btnSplit1.setRequestFocusEnabled(false);
        btnSplit1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSplit1ActionPerformed(evt);
            }
        });

        btnSplit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/refund.png"))); // NOI18N
        btnSplit.setFocusPainted(false);
        btnSplit.setFocusable(false);
        btnSplit.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnSplit.setPreferredSize(new java.awt.Dimension(63, 62));
        btnSplit.setRequestFocusEnabled(false);
        btnSplit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSplitActionPerformed(evt);
            }
        });

        btnCustomer4.setText("subsidio");
        btnCustomer4.setFocusPainted(false);
        btnCustomer4.setFocusable(false);
        btnCustomer4.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer4.setPreferredSize(new java.awt.Dimension(83, 62));
        btnCustomer4.setRequestFocusEnabled(false);
        btnCustomer4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer4ActionPerformed(evt);
            }
        });

        setBackground(new java.awt.Color(255, 204, 153));
        setLayout(new java.awt.CardLayout());

        m_jPanContainer.setLayout(new java.awt.BorderLayout());

        m_jOptions.setLayout(new java.awt.BorderLayout());

        m_jButtons.setPreferredSize(new java.awt.Dimension(810, 62));
        m_jButtons.setRequestFocusEnabled(false);

        m_jTicketId.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        m_jTicketId.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jTicketId.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jTicketId.setOpaque(true);
        m_jTicketId.setPreferredSize(new java.awt.Dimension(290, 50));
        m_jTicketId.setRequestFocusEnabled(false);
        m_jButtons.add(m_jTicketId);

        m_jDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/locationbar_erase.png"))); // NOI18N
        m_jDelete.setFocusPainted(false);
        m_jDelete.setFocusable(false);
        m_jDelete.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jDelete.setPreferredSize(new java.awt.Dimension(63, 62));
        m_jDelete.setRequestFocusEnabled(false);
        m_jDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jDeleteActionPerformed(evt);
            }
        });
        m_jButtons.add(m_jDelete);

        m_jList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search22.png"))); // NOI18N
        m_jList.setFocusPainted(false);
        m_jList.setFocusable(false);
        m_jList.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jList.setPreferredSize(new java.awt.Dimension(63, 62));
        m_jList.setRequestFocusEnabled(false);
        m_jList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jListActionPerformed(evt);
            }
        });
        m_jButtons.add(m_jList);

        m_jEditLine.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/color_line.png"))); // NOI18N
        m_jEditLine.setFocusPainted(false);
        m_jEditLine.setFocusable(false);
        m_jEditLine.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jEditLine.setPreferredSize(new java.awt.Dimension(63, 62));
        m_jEditLine.setRequestFocusEnabled(false);
        m_jEditLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEditLineActionPerformed(evt);
            }
        });
        m_jButtons.add(m_jEditLine);

        jEditAttributes2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/edit_group.png"))); // NOI18N
        jEditAttributes2.setFocusPainted(false);
        jEditAttributes2.setFocusable(false);
        jEditAttributes2.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jEditAttributes2.setPreferredSize(new java.awt.Dimension(64, 62));
        jEditAttributes2.setRequestFocusEnabled(false);
        jEditAttributes2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributes2ActionPerformed(evt);
            }
        });
        m_jButtons.add(jEditAttributes2);

        jEditAttributes3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/kdmconfig.png"))); // NOI18N
        jEditAttributes3.setFocusPainted(false);
        jEditAttributes3.setFocusable(false);
        jEditAttributes3.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jEditAttributes3.setPreferredSize(new java.awt.Dimension(64, 62));
        jEditAttributes3.setRequestFocusEnabled(false);
        jEditAttributes3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributes3ActionPerformed(evt);
            }
        });
        m_jButtons.add(jEditAttributes3);

        btnCustomer2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/button_ok.png"))); // NOI18N
        btnCustomer2.setFocusPainted(false);
        btnCustomer2.setFocusable(false);
        btnCustomer2.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer2.setPreferredSize(new java.awt.Dimension(63, 62));
        btnCustomer2.setRequestFocusEnabled(false);
        btnCustomer2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer2ActionPerformed(evt);
            }
        });
        m_jButtons.add(btnCustomer2);

        m_jOptions.add(m_jButtons, java.awt.BorderLayout.LINE_START);

        m_jPanelScripts.setLayout(new java.awt.BorderLayout());

        m_jButtonsExt.setLayout(new javax.swing.BoxLayout(m_jButtonsExt, javax.swing.BoxLayout.LINE_AXIS));

        jPanel1.setPreferredSize(new java.awt.Dimension(69, 66));

        m_jbtnScale1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/ark2.png"))); // NOI18N
        m_jbtnScale1.setFocusPainted(false);
        m_jbtnScale1.setFocusable(false);
        m_jbtnScale1.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jbtnScale1.setPreferredSize(new java.awt.Dimension(56, 56));
        m_jbtnScale1.setRequestFocusEnabled(false);
        m_jbtnScale1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtnScale1ActionPerformed(evt);
            }
        });
        jPanel1.add(m_jbtnScale1);

        m_jButtonsExt.add(jPanel1);

        m_jPanelScripts.add(m_jButtonsExt, java.awt.BorderLayout.LINE_END);

        m_jOptions.add(m_jPanelScripts, java.awt.BorderLayout.LINE_END);

        m_jPanelBag.setPreferredSize(new java.awt.Dimension(84, 41));
        m_jPanelBag.setLayout(new java.awt.BorderLayout());

        m_jContEntries.setLayout(new java.awt.BorderLayout());

        m_jPanEntries.setLayout(new javax.swing.BoxLayout(m_jPanEntries, javax.swing.BoxLayout.Y_AXIS));

        jPanel9.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel9.setPreferredSize(new java.awt.Dimension(128, 62));
        jPanel9.setLayout(new java.awt.GridBagLayout());

        m_jPrice.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jPrice.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jPrice.setOpaque(true);
        m_jPrice.setPreferredSize(new java.awt.Dimension(110, 22));
        m_jPrice.setRequestFocusEnabled(false);
        jPanel9.add(m_jPrice, new java.awt.GridBagConstraints());

        m_jPanEntries.add(jPanel9);

        m_jKeyFactory.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory.setForeground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory.setBorder(null);
        m_jKeyFactory.setCaretColor(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory.setPreferredSize(new java.awt.Dimension(1, 1));
        m_jKeyFactory.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                m_jKeyFactoryKeyTyped(evt);
            }
        });
        m_jPanEntries.add(m_jKeyFactory);

        m_jContEntries.add(m_jPanEntries, java.awt.BorderLayout.NORTH);

        m_jPanelBag.add(m_jContEntries, java.awt.BorderLayout.LINE_END);

        m_jOptions.add(m_jPanelBag, java.awt.BorderLayout.CENTER);

        m_jPanContainer.add(m_jOptions, java.awt.BorderLayout.NORTH);

        m_jPanTicket.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        m_jPanTicket.setLayout(new java.awt.BorderLayout());

        m_jPanelCentral.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.BorderLayout());

        m_jPanTotals.setLayout(new java.awt.GridBagLayout());

        m_jTotalEuros.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        m_jTotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        m_jTotalEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jTotalEuros.setOpaque(true);
        m_jTotalEuros.setPreferredSize(new java.awt.Dimension(150, 25));
        m_jTotalEuros.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        m_jPanTotals.add(m_jTotalEuros, gridBagConstraints);

        m_jLblTotalEuros1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        m_jLblTotalEuros1.setText(AppLocal.getIntString("label.totalcash")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        m_jPanTotals.add(m_jLblTotalEuros1, gridBagConstraints);

        jPanel4.add(m_jPanTotals, java.awt.BorderLayout.LINE_END);

        m_jPanelCentral.add(jPanel4, java.awt.BorderLayout.SOUTH);

        m_jPanTicket.add(m_jPanelCentral, java.awt.BorderLayout.CENTER);

        m_jContEntries1.setLayout(new java.awt.BorderLayout());

        m_jPanEntries1.setLayout(new javax.swing.BoxLayout(m_jPanEntries1, javax.swing.BoxLayout.Y_AXIS));

        m_jNumberKeys1.setPreferredSize(new java.awt.Dimension(289, 330));
        m_jNumberKeys1.addJNumberEventListener(new com.openbravo.beans.JNumberEventListener() {
            public void keyPerformed(com.openbravo.beans.JNumberEvent evt) {
                m_jNumberKeys1KeyPerformed(evt);
            }
        });
        m_jPanEntries1.add(m_jNumberKeys1);

        jPanel10.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel10.setLayout(new java.awt.GridBagLayout());
        m_jPanEntries1.add(jPanel10);

        m_jKeyFactory1.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory1.setForeground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory1.setBorder(null);
        m_jKeyFactory1.setCaretColor(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory1.setPreferredSize(new java.awt.Dimension(1, 1));
        m_jKeyFactory1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                m_jKeyFactory1KeyTyped(evt);
            }
        });
        m_jPanEntries1.add(m_jKeyFactory1);

        m_jContEntries1.add(m_jPanEntries1, java.awt.BorderLayout.NORTH);

        m_jPanTicket.add(m_jContEntries1, java.awt.BorderLayout.LINE_END);

        m_jPanContainer.add(m_jPanTicket, java.awt.BorderLayout.CENTER);

        catcontainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        catcontainer.setLayout(new java.awt.BorderLayout());
        m_jPanContainer.add(catcontainer, java.awt.BorderLayout.SOUTH);

        add(m_jPanContainer, "ticket");
    }// </editor-fold>//GEN-END:initComponents

    private void m_jbtnScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtnScaleActionPerformed

        //stateTransition('\u00a7');
        int i = m_ticketlines.getSelectedIndex();
        if (i < 0){
            Toolkit.getDefaultToolkit().beep(); // no line selected
        } else {
            String   comentario=     JOptionPane.showInputDialog("Comentario:");

            m_oTicket.getLine(i).setProperty("comentario",comentario);
            refreshTicket();
        }
    }//GEN-LAST:event_m_jbtnScaleActionPerformed

    private void m_jEditLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jEditLineActionPerformed

        int i = m_ticketlines.getSelectedIndex();
        if (i < 0){
            Toolkit.getDefaultToolkit().beep(); // no line selected
        } else {
            /*if(m_App.getAppUserView().getUser().getUserInfo().getName().contains("Administrador"))
            {*/
            try {
                TicketLineInfo newline = JProductLineEdit.showMessage(this, m_App, m_oTicket.getLine(i));
                if (newline != null) {
                    // line has been modified
                    paintTicketLine(i, newline);
                }
            } catch (BasicException e) {
                new MessageInf(e).show(this);
            }
            //}
        }

    }//GEN-LAST:event_m_jEditLineActionPerformed

    private void m_jEnterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jEnterActionPerformed

        stateTransition('\n');

    }//GEN-LAST:event_m_jEnterActionPerformed

    private void m_jNumberKeysKeyPerformed(com.openbravo.beans.JNumberEvent evt) {//GEN-FIRST:event_m_jNumberKeysKeyPerformed

        stateTransition(evt.getKey());

    }//GEN-LAST:event_m_jNumberKeysKeyPerformed

    private void m_jKeyFactoryKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_m_jKeyFactoryKeyTyped

        m_jKeyFactory.setText(null);
        stateTransition(evt.getKeyChar());

    }//GEN-LAST:event_m_jKeyFactoryKeyTyped

    private void m_jDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jDeleteActionPerformed
        
            int i = m_ticketlines.getSelectedIndex();
            if (i < 0){
            Toolkit.getDefaultToolkit().beep(); // No hay ninguna seleccionada
            } else {
            removeTicketLine(i); // elimino la linea
            }  
            /*m_oTicket.setUser(m_App.getAppUserView().getUser().getUserInfo()); // El usuario que lo cobra
            dlSales.setAbreCajon(m_oTicket);
            printTicket("Printer.Ticket2", m_oTicket, null);
        } catch (BasicException ex) {
            Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }//GEN-LAST:event_m_jDeleteActionPerformed

    private void m_jUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jUpActionPerformed
        
        m_ticketlines.selectionUp();

    }//GEN-LAST:event_m_jUpActionPerformed

    private void m_jDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jDownActionPerformed

        m_ticketlines.selectionDown();

    }//GEN-LAST:event_m_jDownActionPerformed

    private void m_jListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jListActionPerformed

        ProductInfoExt prod = JProductFinder.showMessage(JPanelTicket.this, dlSales);    
        if (prod != null) {
            buttonTransition(prod);
        }
        
    }//GEN-LAST:event_m_jListActionPerformed

    private void btnCustomerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomerActionPerformed

        JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, dlCustomers);
        finder.search(m_oTicket.getCustomer());
        finder.setVisible(true);
        
        try {
            m_oTicket.setCustomer(finder.getSelectedCustomer() == null
                    ? null
                    : dlSales.loadCustomerExt(finder.getSelectedCustomer().getId()));
        } catch (BasicException e) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotfindcustomer"), e);
            msg.show(this);            
        }

        refreshTicket();
        
            /*JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, this.dlCustomers);
    finder.search(this.m_oTicket.getCustomer());
    finder.setVisible(true);
    if (finder.getSelectedCustomer() != null) {
      try
      {
        CustomerInfoExt temp = this.dlSales.findCustomerExtById(finder.getSelectedCustomer().getId());
        ArrayList mensajeList = new ArrayList();
        mensajeList.add(new JLabel("Nombre*: "));
        JTextField nombre = new JTextField(temp.getName());
        mensajeList.add(nombre);
        mensajeList.add(new JLabel("Estado*: "));
        JTextField estado = new JTextField();
        mensajeList.add(estado);
        
        mensajeList.add(new JLabel("Ciudad*: "));
        JTextField ciudad = new JTextField();
        mensajeList.add(ciudad);
        
        mensajeList.add(new JLabel("CP*: "));
        JTextField cp = new JTextField();
        mensajeList.add(cp);
        mensajeList.add(new JLabel("Direccion*: "));
        JTextField direccion = new JTextField(temp.getAddress());
        mensajeList.add(direccion);
        mensajeList.add(new JLabel("Referencia: "));
        JTextField referencia = new JTextField(temp.getAddress2());
        mensajeList.add(referencia);
        mensajeList.add(new JLabel("Telefono*: "));
        JTextField telefono = new JTextField(temp.getPhone());
        telefono.setEnabled(false);
        mensajeList.add(telefono);
        mensajeList.add(new JLabel("Telefono 2: "));
        JTextField celular = new JTextField(temp.getPhone2());
        mensajeList.add(celular);
        telefono.addKeyListener(new KeyAdapter()
        {
          public void keyTyped(KeyEvent e)
          {
            char caracter = e.getKeyChar();
            if (((caracter < '0') || (caracter > '9')) && (caracter != '\b')) {
              e.consume();
            }
          }
        });
        mensajeList.add(new JLabel("Observaciones: "));
        JTextField observaciones = new JTextField(temp.getNotes());
        mensajeList.add(observaciones);
        
        mensajeList.add(new JLabel("Deuda Máxima: "));
        JTextField deudaMax = new JTextField(temp.getMaxdebt().toString());
        mensajeList.add(observaciones);
        
        Object[] objetosList2 = mensajeList.toArray();
        String[] opciones2 = { "Aceptar", "Cancelar" };
        objetosList2 = mensajeList.toArray();
        int respuesta2 = JOptionPane.showOptionDialog(null, objetosList2, "Captura de informacion del cliente.", 0, 3, null, opciones2, nombre);
        if (respuesta2 == 0)
        {
          try
          {
            performCustomerUpdate2(telefono.getText(), nombre.getText(), direccion.getText(), referencia.getText(), telefono.getText(), celular.getText(), observaciones.getText(), estado.getText(), ciudad.getText(), cp.getText(),deudaMax.getText().replace("$", "").replace(",", ""));
          }
          catch (BasicException e) {}
          refreshTicket();
        }
        else
        {
          this.m_oTicket.setCustomer(null);
        }
      }
      catch (BasicException ex) {}
    } else {
      this.m_oTicket.setCustomer(null);
    }
    refreshTicket();*/
}//GEN-LAST:event_btnCustomerActionPerformed

    private void performCustomerUpdate(String id, String nombre, String deuda, String activo, String empresa, String tarjeta)
    throws BasicException
  {
    new StaticSentence(this.m_App.getSession(), "UPDATE CUSTOMERS SET NAME = ?, MAXDEBT=?, VISIBLE=?, ADDRESS=?, MODDATE=NOW(), SEARCHKEY=? WHERE ID=?", new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.INT, Datas.STRING, Datas.STRING, Datas.STRING  })).exec(new Object[] { nombre, deuda.replace("$", "").replace(",", ""), activo.equals("Activo")?1:0, empresa, tarjeta, id });
    int status=activo.equals("Activo")?1:0;
    if(status==1)
    this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(id));
    else {this.m_oTicket.setCustomer(null);JOptionPane.showMessageDialog(this, "Se inactivo al empleado");}
  }
    
      private void performCustomerUpdate2(String id, String nombre, String direccion, String referencia, String telefono, String celular, String observaciones, String estado, String ciudad, String cp, String deuda)
    throws BasicException
  {
    new StaticSentence(this.m_App.getSession(), "UPDATE CUSTOMERS SET NAME = ?, ADDRESS=?, ADDRESS2=?, PHONE=?, PHONE2=?, NOTES=?, REGION=?, CITY=?, POSTAL=?, MAXDEBT=? WHERE ID=?", new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING })).exec(new Object[] { nombre, direccion, referencia, telefono, celular, observaciones,estado,ciudad,cp,deuda, id });
    
    this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(id));
  }
    
    private void btnSplitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSplitActionPerformed
/*if ((this.m_oTicket.getLinesCount() > 0) && (this.m_App.getAppUserView().getUser().getRole().equals("0")))
    {
      for (TicketLineInfo line : this.m_oTicket.getLines()) {
        if (line.getProductName().contains("DESCUENTO"))
        {
          JOptionPane.showMessageDialog(this, "Esta cuenta ya contiene un descuento.");
          return;
        }
      }
      double parseDouble = 0.0D;
      try
      {
        String propina = JOptionPane.showInputDialog("Escriba el % de descuento:");
        parseDouble = Double.parseDouble(propina);
      }
      catch (NumberFormatException e1)
      {
        JOptionPane.showMessageDialog(this, "Error.");
        return;
      }
      try
      {
        ProductInfoExt prod = this.dlSales.getProductInfoByReference("0");
        prod.setName("DESCUENTO " + String.valueOf(parseDouble) + "%");
        parseDouble /= 100.0D;
        TaxInfo t = new TaxInfo("000", "Tax Exempt", "000", new Date(), null, null, 0.0D, false, Integer.valueOf(0));
        this.m_oTicket.addLine(new TicketLineInfo(prod, 1.0D, -(parseDouble * this.m_oTicket.getTotal()), t, new Properties()));
        refreshTicket();
      }
      catch (BasicException ex)
      {
        Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
      }
    }*/
int i = m_ticketlines.getSelectedIndex();
        if (i < 0){
            Toolkit.getDefaultToolkit().beep(); // no line selected
        } else {
            String   comentario=     JOptionPane.showInputDialog("Comentario:");

            m_oTicket.getLine(i).setProperty("comentario",comentario);
            refreshTicket();
        } 
}//GEN-LAST:event_btnSplitActionPerformed

    private void jEditAttributesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditAttributesActionPerformed

        int i = m_ticketlines.getSelectedIndex();
        if (i < 0) {
            Toolkit.getDefaultToolkit().beep(); // no line selected
        } else {
            try {
                TicketLineInfo line = m_oTicket.getLine(i);
                JProductAttEdit attedit = JProductAttEdit.getAttributesEditor(this, m_App.getSession());
                attedit.editAttributes(line.getProductAttSetId(), line.getProductAttSetInstId());
                attedit.setVisible(true);
                if (attedit.isOK()) {
                    // The user pressed OK
                    line.setProductAttSetInstId(attedit.getAttributeSetInst());
                    line.setProductAttSetInstDesc(attedit.getAttributeSetInstDescription());
                    paintTicketLine(i, line);
                }
            } catch (BasicException ex) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotfindattributes"), ex);
                msg.show(this);
            }
        }
        
}//GEN-LAST:event_jEditAttributesActionPerformed

    private void btnSplit1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSplit1ActionPerformed
if ((this.m_oTicket.getLinesCount() > 0))
    {
      /*for (TicketLineInfo line : this.m_oTicket.getLines()) {
        if (line.getProductName().contains("DESCUENTO"))
        {
          JOptionPane.showMessageDialog(this, "Esta cuenta ya contiene un incremento.");
          return;
        }
      }*/
      double parseDouble = 0.0D;
      try
      {
        String propina = JOptionPane.showInputDialog("Escriba el % de incremento:");
        parseDouble = Double.parseDouble(propina);
      }
      catch (NumberFormatException e1)
      {
        JOptionPane.showMessageDialog(this, "Error.");
        return;
      }
      try
      {
        ProductInfoExt prod = this.dlSales.getProductInfoByReference("0");
        prod.setName("INCREMENTO " + String.valueOf(parseDouble) + "%");
        parseDouble /= 100.0D;
        TaxInfo t = new TaxInfo("000", "Tax Exempt", "000", new Date(), null, null, 0.0D, false, Integer.valueOf(0));
        this.m_oTicket.addLine(new TicketLineInfo(prod, 1.0D, (parseDouble * this.m_oTicket.getTotal()), t, new Properties()));
        refreshTicket();
      }
      catch (BasicException ex)
      {
        Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    }//GEN-LAST:event_btnSplit1ActionPerformed

    private void m_jEditLine1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jEditLine1ActionPerformed
            if (m_oTicket.getLinesCount() > 0) {
                    
                    if (closeTicket(m_oTicket, m_oTicketExt)) {
                        // Ends edition of current receipt
                        m_ticketsbag.deleteTicket();  
                    } else {
                        // repaint current ticket
                        refreshTicket();
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
    }//GEN-LAST:event_m_jEditLine1ActionPerformed

    private void btnCustomer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer1ActionPerformed
        if (this.m_oTicket.getLinesCount() > 0)
        {
            for (TicketLineInfo line : this.m_oTicket.getLines()) {
                if (line.getProductName().contains("DESCUENTO"))
                {
                    JOptionPane.showMessageDialog(this, "Esta cuenta ya contiene un descuento.");
                    return;
                }
            }
            double parseDouble = 0.0D;
            try
            {
                String propina = JOptionPane.showInputDialog("Escriba el % de descuento:");
                parseDouble = Double.parseDouble(propina);
            }
            catch (NumberFormatException e1)
            {
                JOptionPane.showMessageDialog(this, "Error.");
                return;
            }
            try
            {
                ProductInfoExt prod = this.dlSales.getProductInfoByReference("0");
                prod.setName("DESCUENTO " + String.valueOf(parseDouble) + "%");
                parseDouble /= 100.0D;
                TaxInfo t = new TaxInfo("000", "Tax Exempt", "000", new Date(), null, null, 0.0D, false, Integer.valueOf(0));
                //this.m_oTicket.addLine(new TicketLineInfo(prod, 1.0D, -(parseDouble *this.m_oTicket.getLine(m_oTicket.getLinesCount()-1).getSubValue()), t, new Properties()));
                
                this.m_oTicket.addLine(new TicketLineInfo(prod, 1.0D, -(parseDouble * this.m_oTicket.getTotal()), t, new Properties()));
                refreshTicket();
            }
            catch (BasicException ex)
            {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_btnCustomer1ActionPerformed

    private void m_jNumberKeys1KeyPerformed(com.openbravo.beans.JNumberEvent evt) {//GEN-FIRST:event_m_jNumberKeys1KeyPerformed

        stateTransition(evt.getKey());
    }//GEN-LAST:event_m_jNumberKeys1KeyPerformed

    private void m_jKeyFactory1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_m_jKeyFactory1KeyTyped

        m_jKeyFactory.setText(null);
        stateTransition(evt.getKeyChar());
    }//GEN-LAST:event_m_jKeyFactory1KeyTyped

    private void jEditAttributes1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditAttributes1ActionPerformed
        /*ArrayList mensajeList = new ArrayList();

        mensajeList.add(new JLabel("Nombre*: "));
        JTextField nombre = new JTextField();
        mensajeList.add(nombre);

        mensajeList.add(new JLabel("Estado*: "));
        JTextField estado = new JTextField();
        mensajeList.add(estado);
        
        mensajeList.add(new JLabel("Ciudad*: "));
        JTextField ciudad = new JTextField();
        mensajeList.add(ciudad);
        
        mensajeList.add(new JLabel("CP*: "));
        JTextField cp = new JTextField();
        mensajeList.add(cp);
        
        mensajeList.add(new JLabel("Direccion*: "));
        JTextField direccion = new JTextField();
        mensajeList.add(direccion);

        mensajeList.add(new JLabel("Referencia: "));
        JTextField referencia = new JTextField();
        mensajeList.add(referencia);

        mensajeList.add(new JLabel("Telefono*: "));
        JTextField telefono = new JTextField();
        mensajeList.add(telefono);

        mensajeList.add(new JLabel("Telefono 2: "));
        JTextField celular = new JTextField();
        mensajeList.add(celular);

        telefono.addKeyListener(new KeyAdapter()
            {
                public void keyTyped(KeyEvent e)
                {
                    char caracter = e.getKeyChar();
                    if (((caracter < '0') || (caracter > '9')) && (caracter != '\b')) {
                        e.consume();
                    }
                }
            });
            mensajeList.add(new JLabel("Observaciones: "));
            JTextField observaciones = new JTextField();
            mensajeList.add(observaciones);

            mensajeList.add(new JLabel("Deuda Máxima: "));
            JTextField deudaMax = new JTextField("0");
            mensajeList.add(deudaMax);
            
            Object[] objetosList2 = mensajeList.toArray();
            String[] opciones2 = { "Aceptar", "Cancelar" };
            objetosList2 = mensajeList.toArray();
            int respuesta2 = JOptionPane.showOptionDialog(null, objetosList2, "Captura de informacion del cliente.", 0, 3, null, opciones2, nombre);
            if (respuesta2 == 0) {
                if ((!nombre.getText().equals("")) && (!direccion.getText().equals("")) && (!telefono.getText().equals("")))
                {
                    try
                    {
                        if (!this.dlSales.getCustomer(telefono.getText())) {
                            performCustomerAdd2(telefono.getText(), nombre.getText(), direccion.getText(), referencia.getText(), telefono.getText(), celular.getText(), observaciones.getText(),estado.getText(),ciudad.getText(), cp.getText(),deudaMax.getText().replace(",", "").replace("$", ""));
                        } else {
                            JOptionPane.showMessageDialog(null, "Este cliente ya esta registrado.", "Error", 1);
                        }
                    }
                    catch (BasicException e) {}
                    refreshTicket();
                }
                else
                {
                    JOptionPane.showMessageDialog(null, "No se puede agregar al cliente, faltan campos por ingresar", "Error", 1);
                }
            }*/
    }//GEN-LAST:event_jEditAttributes1ActionPerformed

    private void btnCustomer2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer2ActionPerformed
        if (m_oTicket.getLinesCount() > 0) {
                    
                    if (closeTicket(m_oTicket, m_oTicketExt)) {
                        // Ends edition of current receipt
                        m_ticketsbag.deleteTicket();  
                    } else {
                        // repaint current ticket
                        refreshTicket();
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
    }//GEN-LAST:event_btnCustomer2ActionPerformed

    private void m_jbtnScale1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtnScale1ActionPerformed
        stateTransition('\u00a7');
    }//GEN-LAST:event_m_jbtnScale1ActionPerformed

    private void jEditAttributes2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditAttributes2ActionPerformed
    ArrayList mensajeList = new ArrayList();
    
    mensajeList.add(new JLabel("Número de empleado*: "));
        JTextField nomina = new JTextField();
        mensajeList.add(nomina);
        mensajeList.add(new JLabel("Número de tarjeta*: "));
        JTextField tarjeta = new JTextField();
        mensajeList.add(tarjeta);
        mensajeList.add(new JLabel("Nombre*: "));
        JTextField nombre = new JTextField();
        mensajeList.add(nombre);
        mensajeList.add(new JLabel("Deuda Maxima*: "));
        JTextField deuda = new JTextField("1000.00");
        mensajeList.add(deuda);
        mensajeList.add(new JLabel("Empresa*: "));
        JComboBox empresa = new JComboBox();
        empresa.addItem("BAJIOQ SERVICIOS TECNICOS COREY, S.A. C.V.");
        empresa.addItem("MEX SERVICIOS INTEGRALES COREY, S.A.CV");
        empresa.addItem("MEX SERVICIOS TECNICOS COREY SA DE CV");
        empresa.addItem("STO INDUSTRIAS RECAL SA DE CV");
        empresa.addItem("STO SERVICIOS INTEGRALES COREY, S.A. DE C.V.");
        empresa.addItem("STOQ SERVICIOS TECNICOS COREY, S.A. C.V.");
        empresa.addItem("STOS LOGISTICA CORCAL, SA DE CV");
        empresa.addItem("STOS SERVICIOS TECNICOS COREY, S.A. DE C.V.");
        mensajeList.add(empresa);
        

    Object[] objetosList2 = mensajeList.toArray();
    String[] opciones2 = { "Aceptar", "Cancelar" };
    objetosList2 = mensajeList.toArray();
    int respuesta2 = JOptionPane.showOptionDialog(null, objetosList2, "Nuevo cliente.", 0, 3, null, opciones2, nombre);
    if (respuesta2 == 0) {
      if ((!nombre.getText().equals("")) && (!tarjeta.getText().equals("")) && (!nomina.getText().equals("")))
      {
        try
        {
          if (!this.dlSales.getCustomer(nomina.getText())) {
                    performCustomerAdd(nomina.getText(), tarjeta.getText(), nombre.getText(), deuda.getText(), empresa.getSelectedItem().toString());
          } else {
            JOptionPane.showMessageDialog(null, "Este cliente ya esta registrado.", "Error", 1);
          }
        }
        catch (BasicException localBasicException) {}
        refreshTicket();
      }
      else
      {
        JOptionPane.showMessageDialog(null, "No se puede agregar al cliente, faltan campos por ingresar", "Error", 1);
      }
    }
    }//GEN-LAST:event_jEditAttributes2ActionPerformed

    private void jEditAttributes3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditAttributes3ActionPerformed
    JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, this.dlCustomers);
    finder.search(this.m_oTicket.getCustomer());
    finder.setVisible(true);
    if (finder.getSelectedCustomer() != null) {
      try
      {
        CustomerInfoExt temp = this.dlSales.findCustomerExtById(finder.getSelectedCustomer().getId());
        if(temp!=null)
        {
        ArrayList mensajeList = new ArrayList();
        mensajeList.add(new JLabel("Número de empleado*: "));
        JTextField nomina = new JTextField(temp.getId());
        nomina.setEnabled(false);
        mensajeList.add(nomina);
        mensajeList.add(new JLabel("Número de tarjeta*: "));
        JTextField tarjeta = new JTextField(temp.getSearchkey());
        mensajeList.add(tarjeta);
        mensajeList.add(new JLabel("Nombre*: "));
        JTextField nombre = new JTextField(temp.getName());
        mensajeList.add(nombre);
        mensajeList.add(new JLabel("Deuda Maxima*: "));
        JTextField deuda = new JTextField(temp.printMaxDebt());
        mensajeList.add(deuda);
        mensajeList.add(new JLabel("Deuda actual*: "));
        JTextField deudaActual = new JTextField(temp.printCurDebt());
        deudaActual.setEnabled(false);
        mensajeList.add(deudaActual);
        mensajeList.add(new JLabel("Empresa*: "));
        JComboBox empresa = new JComboBox();
        empresa.addItem("BAJIOQ SERVICIOS TECNICOS COREY, S.A. C.V.");
        empresa.addItem("MEX SERVICIOS INTEGRALES COREY, S.A.CV");
        empresa.addItem("MEX SERVICIOS TECNICOS COREY SA DE CV");
        empresa.addItem("STO INDUSTRIAS RECAL SA DE CV");
        empresa.addItem("STO SERVICIOS INTEGRALES COREY, S.A. DE C.V.");
        empresa.addItem("STOQ SERVICIOS TECNICOS COREY, S.A. C.V.");
        empresa.addItem("STOS LOGISTICA CORCAL, SA DE CV");
        empresa.addItem("STOS SERVICIOS TECNICOS COREY, S.A. DE C.V.");
        empresa.setSelectedItem(temp.getAddress());
        mensajeList.add(empresa);
        mensajeList.add(new JLabel("Activo?: "));
        JComboBox activo = new JComboBox();
        activo.addItem("Activo");
        activo.addItem("Inactivo");
        String status = temp.isVisible()?"Activo":"Inactivo";
        activo.setSelectedItem(status);
        mensajeList.add(activo);
        
        Object[] objetosList2 = mensajeList.toArray();
        String[] opciones2 = { "Aceptar", "Cancelar" };
        objetosList2 = mensajeList.toArray();
        int respuesta2 = JOptionPane.showOptionDialog(null, objetosList2, "Modificar clientes.", 0, 3, null, opciones2, nomina);
        if (respuesta2 == 0)
        {
          try
          {
            performCustomerUpdate(nomina.getText(), nombre.getText(), deuda.getText(), activo.getSelectedItem().toString(), empresa.getSelectedItem().toString(), tarjeta.getText());
          }
          catch (BasicException localBasicException) {}
          refreshTicket();
        }
        else
        {
          this.m_oTicket.setCustomer(null);
        }
      }
      }
      catch (BasicException localBasicException1) {}
    } else {
      this.m_oTicket.setCustomer(null);
    }
    refreshTicket();
    }//GEN-LAST:event_jEditAttributes3ActionPerformed

    private void btnSplit2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSplit2ActionPerformed
int i = m_ticketlines.getSelectedIndex();
        if (i < 0){
            Toolkit.getDefaultToolkit().beep(); // no line selected
        } else {
            String   comentario=     JOptionPane.showInputDialog("Comentario:");

            m_oTicket.getLine(i).setProperty("comentario",comentario);
            refreshTicket();
        }        // TODO add your handling code here:
    }//GEN-LAST:event_btnSplit2ActionPerformed

    private void btnCustomer3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer3ActionPerformed
        JCustomerFinder1 finder = JCustomerFinder1.getCustomerFinder(this, this.dlCustomers);
        finder.search(this.m_oTicket.getCustomer());
        finder.setVisible(true);
        if (finder.getSelectedCustomer() != null) {
                ArrayList mensajeList = new ArrayList();
                mensajeList.add(new JLabel("Tipo: "));
                String[] tipoStrings = { "Compra", "Pago" };
                JComboBox tipo = new JComboBox(tipoStrings);
                mensajeList.add(tipo);
                
                mensajeList.add(new JLabel("Proveedor: "));
                JTextField proveedor = new JTextField(finder.getSelectedCustomer().getName());
                JTextField proveedorid = new JTextField(finder.getSelectedCustomer().getId());
                proveedorid.setVisible(false);
                proveedor.setEnabled(false);
                mensajeList.add(proveedor);
                
                mensajeList.add(new JLabel("Forma de pago: "));
                String[] formaStrings = { "Credito", "Efectivo" };
                JComboBox forma = new JComboBox(formaStrings);
                mensajeList.add(forma);
                
                mensajeList.add(new JLabel("Total: "));
                JTextField total = new JTextField();
                mensajeList.add(total);
                
                
                mensajeList.add(new JLabel("Nota: "));
                JTextField nota = new JTextField();
                mensajeList.add(nota);
                Object[] objetosList2 = mensajeList.toArray();
                    String[] opciones2 = { "Aceptar", "Cancelar" };
                    objetosList2 = mensajeList.toArray();
                    int respuesta2 = JOptionPane.showOptionDialog(null, objetosList2, "Captura de informacion del cliente.", 0, 3, null, opciones2, proveedor);
                    if (respuesta2 == 0)
                    {
                if(tryParseDouble(total.getText().replace(",", "").replace("$","")))
                {
                    
                        try
                        {
                            performProveedorInsert(tipo.getSelectedItem().toString(),proveedorid.getText(),forma.getSelectedItem().toString(),total.getText(),nota.getText());
                        }
                        catch (BasicException e) {}
                        refreshTicket();
                    }
                }
        }            // TODO add your handling code here:
    }//GEN-LAST:event_btnCustomer3ActionPerformed

    private void btnCustomer4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer4ActionPerformed
        if (m_oTicket.getLinesCount() == 0&&m_oTicket.getCustomer()!=null) {
            try {
                ProductInfoExt prod = this.dlSales.getProductInfoByReference("0");
                prod.setName("SUBSIDIO");
                TaxInfo t = new TaxInfo("000", "Tax Exempt", "000", new Date(), null, null, 0.0D, false, Integer.valueOf(0));
                this.m_oTicket.addLine(new TicketLineInfo(prod, 1.0D, 20, t, new Properties()));
                refreshTicket();
                if (closeTicket2(m_oTicket, m_oTicketExt)) {
                    // Ends edition of current receipt
                    m_ticketsbag.deleteTicket();
                } else {
                    // repaint current ticket
                    refreshTicket();
                }
            } catch (BasicException ex) {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
        
    }//GEN-LAST:event_btnCustomer4ActionPerformed
                               
private void performProveedorInsert(String tipo, String proveedor, String formaPago, String total, String nota)
    throws BasicException
  {
      String id=UUID.randomUUID().toString();
      try{
      new PreparedSentence(this.m_App.getSession()
                    , "INSERT INTO RECEIPTS (ID, MONEY, DATENEW) VALUES (?, ?, ?)"
                    , SerializerWriteParams.INSTANCE
                    ).exec(new DataParams() { public void writeValues() throws BasicException {
                        setString(1,id );
                        setString(2, m_App.getActiveCashIndex());
                        setTimestamp(3, new Date());
                    }});
      new PreparedSentence(this.m_App.getSession()
                    , "INSERT INTO PAYMENTS (ID, RECEIPT, PAYMENT, TOTAL, NOTES, FORMAPAGO, PROVEEDOR) VALUES (?, ?, ?, ?, ?,?, ?)"
                    , SerializerWriteParams.INSTANCE
                    ).exec(new DataParams() { public void writeValues() throws BasicException {
                        setString(1, UUID.randomUUID().toString());
                        setString(2, m_App.getActiveCashIndex());
                        setString(3, id);
                        setDouble(4, Double.parseDouble(total));
                        setString(5, nota);
                        setString(6, formaPago);
                        setString(7, proveedor);
                    }});
      } catch (Exception e)
      {
          JOptionPane.showMessageDialog(this, e.getMessage());
      }
    //new StaticSentence(this.m_App.getSession(), "UPDATE CUSTOMERS SET MAXDEBT=?, NAME = ?, ADDRESS=?, ADDRESS2=?, PHONE=?, PHONE2=?, NOTES=?, EMAIL=? WHERE ID=?", new SerializerWriteBasic(new Datas[] { Datas.STRING,Datas.STRING,Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING })).exec(new Object[] {deuda.replace("$", ""), nombre, direccion, referencia, telefono, celular, observaciones, correo,id });
    
    //this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(id));
  }
boolean tryParseDouble(String value) {  
     try {  
         Double.parseDouble(value);  
         return true;  
      } catch (NumberFormatException e) {  
         return false;  
      }  
}
/*
private void performCustomerAdd2(String id, String correo, String nombre, String direccion, String referencia, String telefono, String celular, String observaciones, String estado, String ciudad, String cp, String deuda)
    throws BasicException
  {
      new StaticSentence(this.m_App.getSession(), "INSERT INTO CUSTOMERS (ID, SEARCHKEY, EMAIL,NAME, ADDRESS, ADDRESS2, PHONE, PHONE2, NOTES, REGION,CITY,POSTAL, MAXDEBT) VALUES (?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?, ?,?)", new SerializerWriteBasic(new Datas[] {Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING })).exec(new Object[] { id, id, correo, nombre, direccion, referencia, telefono, celular, observaciones, estado, ciudad, cp, deuda });
    
    this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(id));
  
  }*/
/*private void performCustomerAdd(String id, String correo, String nombre, String direccion, String referencia, String telefono, String celular, String observaciones, String deuda)
    throws BasicException
  {
      new StaticSentence(this.m_App.getSession(), "INSERT INTO CUSTOMERS (ID, SEARCHKEY,EMAIL, NAME, ADDRESS, ADDRESS2, PHONE, PHONE2, NOTES, MAXDEBT, VISIBLE) VALUES (?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", 
              new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING,Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING,Datas.DOUBLE, Datas.INT }))
              .exec(new Object[] { id, id, correo,nombre, direccion, referencia, telefono, celular, observaciones, Double.parseDouble( deuda.replace("$", "") ), 1});
    
    this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(id));
  
  }*/
private void performCustomerAdd(String nomina, String tarjeta, String nombre, String deuda, String empresa)
    throws BasicException
  {
    new StaticSentence(this.m_App.getSession(), "INSERT INTO CUSTOMERS (ID, SEARCHKEY,NAME, MAXDEBT,ADDRESS, VISIBLE,NOTES, CREATEDATE) VALUES (?,?, ?, ?, ?, ?,?, NOW())", 
            new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.DOUBLE, Datas.STRING, Datas.INT, Datas.STRING }))
            .exec(new Object[] { nomina, tarjeta, nombre, Double.valueOf(Double.parseDouble(deuda.replace("$", ""))), empresa , Integer.valueOf(1), m_App.getAppUserView().getUser().getName() });
    
    this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(nomina));
  }
  
  /*private void performCustomerUpdate(String id, String nombre, String direccion, String referencia, String telefono, String celular, String observaciones, String deuda, String correo)
    throws BasicException
  {
    new StaticSentence(this.m_App.getSession(), "UPDATE CUSTOMERS SET MAXDEBT=?, NAME = ?, ADDRESS=?, ADDRESS2=?, PHONE=?, PHONE2=?, NOTES=?, EMAIL=? WHERE ID=?", new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING })).exec(new Object[] { deuda.replace("$", ""), nombre, direccion, referencia, telefono, celular, observaciones, correo, id });
    
    this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(id));
  }*/
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCustomer;
    private javax.swing.JButton btnCustomer1;
    private javax.swing.JButton btnCustomer2;
    private javax.swing.JButton btnCustomer3;
    private javax.swing.JButton btnCustomer4;
    private javax.swing.JButton btnSplit;
    private javax.swing.JButton btnSplit1;
    private javax.swing.JButton btnSplit2;
    private javax.swing.JPanel catcontainer;
    private javax.swing.JButton jEditAttributes;
    private javax.swing.JButton jEditAttributes1;
    private javax.swing.JButton jEditAttributes2;
    private javax.swing.JButton jEditAttributes3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPanel m_jButtons;
    private javax.swing.JPanel m_jButtonsExt;
    private javax.swing.JPanel m_jContEntries;
    private javax.swing.JPanel m_jContEntries1;
    private javax.swing.JButton m_jDelete;
    private javax.swing.JButton m_jDown;
    private javax.swing.JButton m_jEditLine;
    private javax.swing.JButton m_jEditLine1;
    private javax.swing.JButton m_jEnter;
    private javax.swing.JTextField m_jKeyFactory;
    private javax.swing.JTextField m_jKeyFactory1;
    private javax.swing.JLabel m_jLblTotalEuros1;
    private javax.swing.JLabel m_jLblTotalEuros2;
    private javax.swing.JLabel m_jLblTotalEuros3;
    private javax.swing.JButton m_jList;
    private com.openbravo.beans.JNumberKeys m_jNumberKeys;
    private com.openbravo.beans.JNumberKeys m_jNumberKeys1;
    private javax.swing.JPanel m_jOptions;
    private javax.swing.JPanel m_jPanContainer;
    private javax.swing.JPanel m_jPanEntries;
    private javax.swing.JPanel m_jPanEntries1;
    private javax.swing.JPanel m_jPanTicket;
    private javax.swing.JPanel m_jPanTotals;
    private javax.swing.JPanel m_jPanelBag;
    private javax.swing.JPanel m_jPanelCentral;
    private javax.swing.JPanel m_jPanelScripts;
    private javax.swing.JLabel m_jPor;
    private javax.swing.JLabel m_jPrice;
    private javax.swing.JLabel m_jSubtotalEuros;
    private javax.swing.JComboBox m_jTax;
    private javax.swing.JLabel m_jTaxesEuros;
    private javax.swing.JLabel m_jTicketId;
    private javax.swing.JLabel m_jTotalEuros;
    private javax.swing.JButton m_jUp;
    private javax.swing.JToggleButton m_jaddtax;
    private javax.swing.JButton m_jbtnScale;
    private javax.swing.JButton m_jbtnScale1;
    // End of variables declaration//GEN-END:variables

}
