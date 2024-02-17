package view;

import components.CustomRender;
import components.Messages;
import components.ValidateField;
import components.reportViewer;
import controller.ConnectToDataBase;
import controller.FillCombo;
import controller.ManagementSessions;
import controller.PurchaseController;
import controller.TableHeader;
import controller.VoucherController;
import gui.Loading;
import gui.SearchProviders;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

import model.Voucher;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 *
 * @author David
 */
public class InternalCreateVoucher extends javax.swing.JInternalFrame {

    /**
     * Creates new form InternalCreateVoucher
     */
    private static InternalCreateVoucher internalCreateVoucher = null;
    private StringBuilder sql = null;
    private String SQL_INSERT;
    private String SQL_INSERT_2;
    private List data;
    
    public static InternalCreateVoucher getInstance() {
        if (internalCreateVoucher == null) {
            internalCreateVoucher = new InternalCreateVoucher();
        }
        
        return internalCreateVoucher;
    }
    
    private PurchaseController purchaseController;
    private VoucherController voucherController;
    private TableHeader tableHeader;
    private int idProvider;
    
    public void setIdProvider(int idProvider) {
        this.idProvider = idProvider;
    }
    
    public void setProviderName(String providerName) {
        companyField.setText(providerName);
    }
    
    public InternalCreateVoucher() {
        initComponents();
        purchaseController = new PurchaseController();
        voucherController = new VoucherController();
        tableHeader = new TableHeader();
        tableHeader.voucherCreation(tablePurchasesAvalaible);
        TableHeader.cleanRowsTable(voucherTableDescription);
        tableHeader.voucherCreationList(voucherTableDescription);
        userName.setText("" + ManagementSessions.getFullName());
        changeCurrency();
        setTitleVoucher();
        
        FillCombo.authorizationSignatures(signaturesGroupCB);
        
    }
    
    private void setCurrencyComission() {
        try {
            if (radioQuetzales.isSelected()) {
                commisionField.setFormatterFactory(new DefaultFormatterFactory(new MaskFormatter("Q#,###,###,###,###.##")));
                
            }
            
            if (radioDollars.isSelected()) {
                commisionField.setFormatterFactory(new DefaultFormatterFactory(new MaskFormatter("$#,###,###,###,###.##")));
            }
        } catch (ParseException ex) {
            Logger.getLogger(InternalCreateVoucher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void listPurchasesAvailable() {
        
        ArrayList<String> criterios = null;
        
        if (radioQuetzales.isSelected()) {
            sql = new StringBuilder("SELECT id, "
                    + "purchaseNumber, "
                    + "creation_date, "
                    + "total "
                    + "FROM ("
                    + "SELECT MAX(pq.id) AS id, "
                    + "CONCAT('Q-', DATE_FORMAT(pq.creation_date, '%y%m%d'), LPAD(pq.purchase_number, 5, '0')) AS purchaseNumber, "
                    + "MAX(pq.creation_date) AS creation_date, "
                    + "MAX(pq.acceptance_date) AS acceptance_date, "
                    + "SUM(dpq.amount) AS total, "
                    + "MAX(CASE WHEN pvdv.id_purchase IS NOT NULL THEN 'YES' ELSE 'NO' END) AS voucher_exist, "
                    + "pq.type_purchase "
                    + "FROM purchases_quetzales pq "
                    + "JOIN description_purchases_quetzales dpq ON pq.id = dpq.purchase "
                    + "JOIN users u ON pq.user = u.id "
                    + "JOIN providers p ON pq.provider = p.id "
                    + "JOIN expense_category_permission ecp ON ecp.expense_category = p.category "
                    + "LEFT JOIN pdf_attach_purchase_order_quetzales AS pdf_attach ON pq.id = pdf_attach.idPurchase "
                    + "LEFT JOIN purchases_vouchers_quetzales_vinculation AS pvdv ON pq.id = pvdv.id_purchase "
                    + "LEFT JOIN voucher_quetzales vq ON pvdv.id_voucher = vq.id "
                    + "WHERE ecp.deparment = ? and p.id = '" + idProvider + "' "
                    + "GROUP BY pq.id"
                    + ") AS subquery "
                    + "WHERE acceptance_date IS NOT NULL "
                    + "AND voucher_exist = 'NO'");
            
        }
        
        if (radioDollars.isSelected()) {
            sql = new StringBuilder("SELECT id, "
                    + "purchaseNumber, "
                    + "creation_date, "
                    + "total "
                    + "FROM ("
                    + "SELECT MAX(pd.id) AS id, "
                    + "CONCAT('$-', DATE_FORMAT(pd.creation_date, '%y%m%d'), LPAD(pd.purchase_number, 5, '0')) AS purchaseNumber, "
                    + "MAX(pd.creation_date) AS creation_date, "
                    + "MAX(pd.acceptance_date) AS acceptance_date, "
                    + "SUM(dpd.amount) AS total, "
                    + "MAX(CASE WHEN pvdv.id_purchase IS NOT NULL THEN 'YES' ELSE 'NO' END) AS voucher_exist, "
                    + "pd.type_purchase "
                    + "FROM purchases_dollars pd "
                    + "JOIN description_purchases_dollars dpd ON pd.id = dpd.purchase "
                    + "JOIN users u ON pd.user = u.id "
                    + "JOIN providers p ON pd.provider = p.id "
                    + "JOIN expense_category_permission ecp ON ecp.expense_category = p.category "
                    + "LEFT JOIN pdf_attach_purchase_order_dollars AS pdf_attach ON pd.id = pdf_attach.idPurchase "
                    + "LEFT JOIN purchases_vouchers_dollars_vinculation AS pvdv ON pd.id = pvdv.id_purchase "
                    + "LEFT JOIN voucher_dollars vd ON pvdv.id_voucher = vd.id "
                    + "WHERE ecp.deparment = ? and p.id = '" + idProvider + "' "
                    + "GROUP BY pd.id"
                    + ") AS subquery "
                    + "WHERE acceptance_date IS NOT NULL "
                    + "AND voucher_exist = 'NO'");
            
        }
        
        System.out.println(sql.toString());
        data = purchaseController.listPurchase(sql.toString());
    }
    
    private void refreshDataTableInBackground() {
        JDialog loadingDialog = createLoadingDialog((JFrame) SwingUtilities.windowForComponent(FrameMain.mainDesk));
        Timer timer = new Timer(2000, (ActionEvent e) -> {
            loadingDialog.dispose();
        });
        timer.setRepeats(false);
        timer.start();
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                listPurchasesAvailable();
                return null;
            }
            
            @Override
            protected void done() {
                
                List list = data;
                DefaultTableModel model = (DefaultTableModel) tablePurchasesAvalaible.getModel();
                model.getDataVector().clear();
                for (int i = 1; i < list.size(); i++) {
                    Object[] row = (Object[]) list.get(i);
                    model.addRow(row);
                    
                }
                model.fireTableDataChanged();
                
                timer.stop();
                loadingDialog.dispose();
            }
        };
        
        worker.execute();
    }
    
    private static JDialog createLoadingDialog(JFrame parent) {
        
        JDialog loadingDialog = new Loading(parent, false);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Evitar que el usuario cierre la notificación
        loadingDialog.setSize(90, 90);
        loadingDialog.setLocationRelativeTo(parent);
        loadingDialog.setAlwaysOnTop(true);
        loadingDialog.setVisible(true);
        return loadingDialog;
    }
    
    private void changeCurrency() {
        if (radioDollars.isSelected()) {
            voucherTableDescription.setDefaultRenderer(Object.class, new CustomRender());
            voucherTableDescription.repaint();
            tablePurchasesAvalaible.setDefaultRenderer(Object.class, new CustomRender());
            tablePurchasesAvalaible.repaint();
            comissionLabel.setText("Comission $:");
        }
        
        if (radioQuetzales.isSelected()) {
            voucherTableDescription.setDefaultRenderer(Object.class, new CustomRender());
            voucherTableDescription.repaint();
            tablePurchasesAvalaible.setDefaultRenderer(Object.class, new CustomRender());
            tablePurchasesAvalaible.repaint();
            comissionLabel.setText("Comission Q:");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        removePurchaseList = new javax.swing.JPopupMenu();
        remove = new javax.swing.JMenuItem();
        paymentMethod = new javax.swing.ButtonGroup();
        currency = new javax.swing.ButtonGroup();
        titleGroupButton = new javax.swing.ButtonGroup();
        typeVoucherGroup = new javax.swing.ButtonGroup();
        amount = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        paymentDateChooser = new com.toedter.calendar.JDateChooser();
        jLabel3 = new javax.swing.JLabel();
        userName = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        amountField = new javax.swing.JFormattedTextField();
        jLabel5 = new javax.swing.JLabel();
        companyField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        cash = new javax.swing.JRadioButton();
        creditCard = new javax.swing.JRadioButton();
        cashierCheck = new javax.swing.JRadioButton();
        jLabel7 = new javax.swing.JLabel();
        titleVoucherField = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablePurchasesAvalaible = new javax.swing.JTable(){
            public boolean isCellEditable(int row, int column){
                return false;
            }
        };
        jLabel8 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        voucherTableDescription = new javax.swing.JTable(){
            public boolean isCellEditable(int row, int column){
                return column == 4 || column == 5;
            }
        };
        jRadioButton4 = new javax.swing.JRadioButton();
        jRadioButton5 = new javax.swing.JRadioButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        remarks = new javax.swing.JTextArea();
        jLabel9 = new javax.swing.JLabel();
        comissionLabel = new javax.swing.JLabel();
        commisionField = new javax.swing.JFormattedTextField();
        jLabel11 = new javax.swing.JLabel();
        gTotalField = new javax.swing.JFormattedTextField();
        jPanel1 = new javax.swing.JPanel();
        radioQuetzales = new javax.swing.JRadioButton();
        radioDollars = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        simpleVoucherRBtn = new javax.swing.JRadioButton();
        detailedVoucherRBtn = new javax.swing.JRadioButton();
        jLabel10 = new javax.swing.JLabel();
        signaturesGroupCB = new javax.swing.JComboBox();

        remove.setText("jMenuItem1");
        remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeActionPerformed(evt);
            }
        });
        removePurchaseList.add(remove);

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Create Voucher");
        setToolTipText("");
        setFrameIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/createVoucher.png"))); // NOI18N
        setName("CreateVoucher"); // NOI18N
        addInternalFrameListener(new javax.swing.event.InternalFrameListener() {
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
                formInternalFrameClosing(evt);
            }
            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameIconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
            }
        });

        amount.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel2.setText("Payment Date:");

        jLabel3.setText("Who creates the voucher?: ");

        userName.setEditable(false);
        userName.setFocusable(false);

        jLabel4.setText("Sub Total:");

        amountField.setEditable(false);
        amountField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        amountField.setFocusable(false);

        jLabel5.setText("Company:");

        companyField.setEditable(false);
        companyField.setFocusable(false);

        jLabel6.setText("Payment Method:");

        paymentMethod.add(cash);
        cash.setSelected(true);
        cash.setText("Cash");

        paymentMethod.add(creditCard);
        creditCard.setText("Credit Card:");

        paymentMethod.add(cashierCheck);
        cashierCheck.setText("Cashier Check");

        jLabel7.setText("Voucher title:");

        titleVoucherField.setEditable(false);
        titleVoucherField.setFocusable(false);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/search.png"))); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        tablePurchasesAvalaible.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tablePurchasesAvalaible.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tablePurchasesAvalaible);

        jLabel8.setText("Purchases avalaible for add to voucher");

        jLabel1.setText(" Purchases added to the voucher:");

        jButton2.setText("Generate Voucher");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/search.png"))); // NOI18N
        jButton3.setToolTipText("Search Purchases");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/add.png"))); // NOI18N
        jButton4.setToolTipText("Add to voucher");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/removeCar.png"))); // NOI18N
        jButton5.setToolTipText("Remove from voucher");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        voucherTableDescription.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        voucherTableDescription.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(voucherTableDescription);

        titleGroupButton.add(jRadioButton4);
        jRadioButton4.setSelected(true);
        jRadioButton4.setText("KNIT");
        jRadioButton4.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioButton4ItemStateChanged(evt);
            }
        });

        titleGroupButton.add(jRadioButton5);
        jRadioButton5.setText("APPAREL");
        jRadioButton5.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioButton5ItemStateChanged(evt);
            }
        });

        remarks.setColumns(20);
        remarks.setLineWrap(true);
        remarks.setRows(5);
        remarks.setTabSize(3);
        remarks.setWrapStyleWord(true);
        jScrollPane2.setViewportView(remarks);

        jLabel9.setText("Remarks:");

        comissionLabel.setText("Comission Q:");

        commisionField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        commisionField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                commisionFieldKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                commisionFieldKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                commisionFieldKeyTyped(evt);
            }
        });

        jLabel11.setText("G. Total:");

        gTotalField.setEditable(false);
        gTotalField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        gTotalField.setFocusable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Currency"));

        currency.add(radioQuetzales);
        radioQuetzales.setSelected(true);
        radioQuetzales.setText("Quetzales");
        radioQuetzales.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                radioQuetzalesItemStateChanged(evt);
            }
        });
        radioQuetzales.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioQuetzalesActionPerformed(evt);
            }
        });

        currency.add(radioDollars);
        radioDollars.setText("Dollars");
        radioDollars.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                radioDollarsItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(radioQuetzales)
                    .addComponent(radioDollars))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(radioQuetzales)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioDollars)
                .addGap(0, 6, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Type Voucher"));

        typeVoucherGroup.add(simpleVoucherRBtn);
        simpleVoucherRBtn.setSelected(true);
        simpleVoucherRBtn.setText("Simple");

        typeVoucherGroup.add(detailedVoucherRBtn);
        detailedVoucherRBtn.setText("Detailed");
        detailedVoucherRBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                detailedVoucherRBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(simpleVoucherRBtn)
                    .addComponent(detailedVoucherRBtn))
                .addContainerGap(12, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(simpleVoucherRBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(detailedVoucherRBtn)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jLabel10.setText("Signature Group:");

        javax.swing.GroupLayout amountLayout = new javax.swing.GroupLayout(amount);
        amount.setLayout(amountLayout);
        amountLayout.setHorizontalGroup(
            amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(amountLayout.createSequentialGroup()
                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(amountLayout.createSequentialGroup()
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(amountLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel9)
                                .addGap(161, 161, 161)
                                .addComponent(jLabel11))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, amountLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel5)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel6))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(amountLayout.createSequentialGroup()
                                        .addComponent(cash)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(creditCard)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cashierCheck)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel4))
                                    .addGroup(amountLayout.createSequentialGroup()
                                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(paymentDateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(amountLayout.createSequentialGroup()
                                                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                    .addComponent(companyField)
                                                    .addComponent(titleVoucherField, javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(userName, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 432, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(amountLayout.createSequentialGroup()
                                                        .addComponent(jRadioButton4)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jRadioButton5))
                                                    .addComponent(jButton1))))
                                        .addGap(0, 0, Short.MAX_VALUE))))
                            .addGroup(amountLayout.createSequentialGroup()
                                .addGap(76, 76, 76)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(signaturesGroupCB, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(comissionLabel)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(amountField)
                            .addComponent(commisionField)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, amountLayout.createSequentialGroup()
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(amountLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(amountLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(amountLayout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(jButton2))
                                    .addGroup(amountLayout.createSequentialGroup()
                                        .addComponent(jButton5)
                                        .addGap(0, 0, Short.MAX_VALUE))))
                            .addComponent(jScrollPane3)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, amountLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(gTotalField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, amountLayout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 458, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        amountLayout.setVerticalGroup(
            amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(amountLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(amountLayout.createSequentialGroup()
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(titleVoucherField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jRadioButton4)
                            .addComponent(jRadioButton5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(userName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(companyField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(jButton1))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(paymentDateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(10, 10, 10)
                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(amountLayout.createSequentialGroup()
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(amountField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comissionLabel)
                            .addComponent(commisionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(gTotalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(amountLayout.createSequentialGroup()
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cash)
                            .addComponent(creditCard)
                            .addComponent(cashierCheck)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(signaturesGroupCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButton3)
                            .addComponent(jLabel8)
                            .addComponent(jButton4)))
                    .addGroup(amountLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel9)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(amountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jButton5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(amount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(amount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>                        

    private void formInternalFrameClosing(javax.swing.event.InternalFrameEvent evt) {                                          
        internalCreateVoucher = null;
    }                                         

    private void removeActionPerformed(java.awt.event.ActionEvent evt) {                                       

    }                                      

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        
        if (voucherTableDescription.getRowCount() == 0) {
            SearchProviders providers = new SearchProviders(
                    (Frame) SwingUtilities.windowForComponent(InternalCreateVoucher.getInstance()), true, this);
            providers.setLocationRelativeTo(InternalCreateVoucher.getInstance());
            providers.setVisible(true);
        } else {
            int option = Messages.confirmation("If you accept the purchases added to the voucher they will be removed ");
            if (JOptionPane.YES_OPTION == option) {
                TableHeader.cleanRowsTable(voucherTableDescription);
                TableHeader.cleanRowsTable(tablePurchasesAvalaible);
                amountField.setText("");
                SearchProviders providers = new SearchProviders((Frame) SwingUtilities.windowForComponent(InternalCreateVoucher.getInstance()), true, InternalCreateVoucher.getInstance());
                providers.setLocationRelativeTo(InternalCreateVoucher.getInstance());
                providers.setVisible(true);
            }
        }
        

    }                                        

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        
        if (voucherTableDescription.getRowCount() == 0) {
            if (idProvider != 0) {
                refreshDataTableInBackground();
            } else {
                Messages.information("Select one company first");
            }
        } else {
            int option = Messages.confirmation("If you accept the purchases added to the voucher they will be removed ");
            if (JOptionPane.YES_OPTION == option) {
                TableHeader.cleanRowsTable(voucherTableDescription);
                TableHeader.cleanRowsTable(tablePurchasesAvalaible);
                amountField.setText("");
                SearchProviders providers = new SearchProviders((Frame) SwingUtilities.windowForComponent(InternalCreateVoucher.getInstance()), true, InternalCreateVoucher.getInstance());
                providers.setLocationRelativeTo(InternalCreateVoucher.getInstance());
                providers.setVisible(true);
            }
        }

    }                                        

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        DefaultTableModel modeloOrigen = (DefaultTableModel) tablePurchasesAvalaible.getModel();
        DefaultTableModel modeloDestino = (DefaultTableModel) voucherTableDescription.getModel();
        
        int filaSeleccionada = tablePurchasesAvalaible.getSelectedRow();
        
        if (filaSeleccionada != -1) {
            Object[] fila = new Object[modeloOrigen.getColumnCount()];
            for (int i = 0; i < modeloOrigen.getColumnCount(); i++) {
                fila[i] = modeloOrigen.getValueAt(filaSeleccionada, i);
            }
            
            modeloDestino.addRow(fila); 
            modeloOrigen.removeRow(filaSeleccionada); 
            calculateAmount();
        } else {
            JOptionPane.showMessageDialog(null, "Select a row to move to the voucher", "Error", JOptionPane.ERROR_MESSAGE);
        }
        

    }                                        

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        
        DefaultTableModel modeloOrigen = (DefaultTableModel) voucherTableDescription.getModel();
        DefaultTableModel modeloDestino = (DefaultTableModel) tablePurchasesAvalaible.getModel();
        
        int filaSeleccionada = voucherTableDescription.getSelectedRow();
        
        if (filaSeleccionada != -1) {
            ArrayList<Object> fila = new ArrayList<>();
            for (int i = 0; i < modeloOrigen.getColumnCount(); i++) {
                fila.add(modeloOrigen.getValueAt(filaSeleccionada, i));
            }
            
            modeloDestino.addRow(fila.toArray());
            
            modeloOrigen.removeRow(filaSeleccionada);
            calculateAmount();
        } else {
            JOptionPane.showMessageDialog(null, "Select a row to move to the voucher", "Error", JOptionPane.ERROR_MESSAGE);
        }

    }                                        
    private boolean isHandlingEvent = false;

    private void radioQuetzalesItemStateChanged(java.awt.event.ItemEvent evt) {                                                
        if (evt.getStateChange() == ItemEvent.SELECTED && !isHandlingEvent) {
            if (tablePurchasesAvalaible.getRowCount() > 0 || voucherTableDescription.getRowCount() > 0) {
                int option = Messages.confirmation("If you accept the purchases added to the voucher they will be removed ");
                if (JOptionPane.YES_OPTION == option) {
                    TableHeader.cleanRowsTable(voucherTableDescription);
                    TableHeader.cleanRowsTable(tablePurchasesAvalaible);
                    amountField.setText("");
                    commisionField.setText("");
                    gTotalField.setText("");
                    
                } else {
                    isHandlingEvent = true;
                    radioDollars.setSelected(true);
                    isHandlingEvent = false;
                }
            }
            changeCurrency();
        }
    }                                               

    private void radioDollarsItemStateChanged(java.awt.event.ItemEvent evt) {                                              
        if (evt.getStateChange() == ItemEvent.SELECTED && !isHandlingEvent) {
            if (tablePurchasesAvalaible.getRowCount() > 0 || voucherTableDescription.getRowCount() > 0) {
                int option = Messages.confirmation("If you accept the purchases added to the voucher they will be removed ");
                if (JOptionPane.YES_OPTION == option) {
                    TableHeader.cleanRowsTable(voucherTableDescription);
                    TableHeader.cleanRowsTable(tablePurchasesAvalaible);
                    amountField.setText("");
                    commisionField.setText("");
                    gTotalField.setText("");
                } else {
                    
                    isHandlingEvent = true;
                    radioQuetzales.setSelected(true);
                    isHandlingEvent = false;
                }
            }
            changeCurrency();
        }
    }                                             

    private void radioQuetzalesActionPerformed(java.awt.event.ActionEvent evt) {                                               
        // TODO add your handling code here:
    }                                              
    
    private boolean generateVoucher() {
        
        if (radioDollars.isSelected()) {
            SQL_INSERT = "INSERT INTO voucher_dollars (voucher_number,id_user_created,created_date, status, payment_date,voucher_header, remarks, type_payment,type_voucher, comission, signature_group ) "
                    + "(SELECT coalesce(MAX(voucher_number),0)+1, ?, NOW(), ?, ?, ?, ?, ?,?,?,? FROM voucher_dollars FOR UPDATE)";
            SQL_INSERT_2 = "INSERT INTO purchases_vouchers_dollars_vinculation (id_purchase, id_voucher, invoices, invoice_date) VALUES(?,?,?,?)";
        }
        
        if (radioQuetzales.isSelected()) {
            
            SQL_INSERT = "INSERT INTO voucher_quetzales (voucher_number,id_user_created,created_date, status, payment_date,voucher_header, remarks, type_payment, type_voucher, comission, signature_group) "
                    + "(SELECT coalesce(MAX(voucher_number),0)+1, ?, NOW(), ?, ?, ?, ?, ?,?,?, ? FROM voucher_quetzales FOR UPDATE)";
                SQL_INSERT_2 = "INSERT INTO purchases_vouchers_quetzales_vinculation (id_purchase, id_voucher, invoices, invoice_date) VALUES(?,?,?,?)";
            
        }
        
        Map.Entry<String, Integer> selectedItem = (Map.Entry<String, Integer>) signaturesGroupCB.getSelectedItem();
        String paymentMethod = null;
        String tyoeVoucher = null;
        if (cash.isSelected()) {
            paymentMethod = "Cash";
        }
        
        if (creditCard.isSelected()) {
            paymentMethod = "Credit Card";
        }
        
        if (cashierCheck.isSelected()) {
            paymentMethod = "Cashier Check";
        }
        
        if (simpleVoucherRBtn.isSelected()) {
            tyoeVoucher = "SIMPLE";
        }
        
        if (detailedVoucherRBtn.isSelected()) {
            tyoeVoucher = "DETAILED";
        }
        
        ArrayList<Voucher> listVoucher = new ArrayList<>();
        for (int i = 0; i < voucherTableDescription.getRowCount(); i++) {
            Voucher voucher = new Voucher();
            voucher.setVoucherHeader(titleVoucherField.getText());
            voucher.setIdUserCreated(ManagementSessions.getId());
            voucher.setTypePayment(paymentMethod);
            voucher.setPaymentDate(paymentDateChooser.getDate());
            voucher.setPurchase((int) voucherTableDescription.getValueAt(i, 0));
            Object value = voucherTableDescription.getValueAt(i, 4);
            voucher.setRemarks(remarks.getText());
            Date value2 = (Date) voucherTableDescription.getValueAt(i, 5);
            
            if (value instanceof String && value != null) {
                voucher.setInvoices(value.toString());
            }
            
            if (value2 != null && value2 instanceof Date) {
                voucher.setInvoiceDate((Date) value2);
            }
            voucher.setTypeVoucher(tyoeVoucher);
            
            if (!commisionField.getText().trim().isEmpty()) {
                String valueCommisionField = commisionField.getText().trim();
                valueCommisionField = valueCommisionField.replace(",", "");
                voucher.setComission((Double.parseDouble(valueCommisionField)));
            } else {
                voucher.setComission(0);
            }
            
            voucher.setSignatureGroup(selectedItem.getValue());
            listVoucher.add(voucher);
        }
        
        return voucherController.insertVoucher(listVoucher, SQL_INSERT, SQL_INSERT_2);
    }
    
    private void openReportBackground() {
        SwingWorker<Void, Void> openReport = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                
                if (radioDollars.isSelected()) {
                    openReporDollars(voucherController.getLastInsertId());
                }
                
                if (radioQuetzales.isSelected()) {
                    openReporQuetzales(voucherController.getLastInsertId());
                }
                return null;
            }
            
        };
        openReport.execute();
    }
    
    public void openReporDollars(int id) throws SQLException {
        try (Connection cn = ConnectToDataBase.getInstance().getConnection()) {
            JasperReport jasper;
            JasperReport jasper2;
            
            InputStream inputStream = getClass().getResourceAsStream("/reports/voucher_dollars_main.jasper");
            jasper = (JasperReport) JRLoader.loadObject(inputStream);
            
            InputStream inputStream2 = getClass().getResourceAsStream("/reports/voucher_dollars_sub.jasper");
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("searchId", id);
            parameters.put("subreportStream", inputStream2);
            
            JasperPrint print = JasperFillManager.fillReport(jasper, parameters, cn);
            
            reportViewer viewer = new reportViewer(print);
            Report report = new Report();
            report.setPreferredSize(new Dimension(1280, 720));
            report.add(viewer);
            report.pack();
            report.setLocationRelativeTo(SwingUtilities.windowForComponent(report));
            report.setVisible(true);
        } catch (JRException | SQLException ex) {
            Messages.error(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public void openReporQuetzales(int id) {
        try (Connection cn = ConnectToDataBase.getInstance().getConnection()) {
            JasperReport jasper;
            JasperReport jasper2;
            
            InputStream inputStream = getClass().getResourceAsStream("/reports/voucher_quetzales_main.jasper");
            jasper = (JasperReport) JRLoader.loadObject(inputStream);
            
            InputStream inputStream2 = getClass().getResourceAsStream("/reports/voucher_quetzales_sub.jasper");
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("searchId", id);
            parameters.put("subreportStream", inputStream2);
            
            JasperPrint print = JasperFillManager.fillReport(jasper, parameters, cn);
            
            reportViewer viewer = new reportViewer(print);
            Report report = new Report();
            report.setPreferredSize(new Dimension(1280, 720));
            report.add(viewer);
            report.pack();
            report.setLocationRelativeTo(SwingUtilities.windowForComponent(report));
            report.setVisible(true);
        } catch (JRException | SQLException ex) {
            Messages.error(ex.getMessage());
            ex.printStackTrace();
        }
    }
    

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        if (validateFields()) {
            if (generateVoucher()) {
                cleanFields();
                openReportBackground();
            }
        } else {
            Messages.information("You must have at least one purchase on the voucher");
        }
    }                                        
    
    private void cleanFields() {
        remarks.setText("");
        idProvider = 0;
        companyField.setText("");
        commisionField.setText("");
        gTotalField.setText("");
        TableHeader.cleanRowsTable(tablePurchasesAvalaible);
        TableHeader.cleanRowsTable(voucherTableDescription);
        calculateAmount();
        cash.setSelected(true);
        paymentDateChooser.setDate(null);
    }

    private void jRadioButton4ItemStateChanged(java.awt.event.ItemEvent evt) {                                               
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            setTitleVoucher();
        }
    }                                              

    private void jRadioButton5ItemStateChanged(java.awt.event.ItemEvent evt) {                                               
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            setTitleVoucher();
        }
    }                                              

    private void detailedVoucherRBtnActionPerformed(java.awt.event.ActionEvent evt) {                                                    
        // TODO add your handling code here:
    }                                                   

    private void commisionFieldKeyTyped(java.awt.event.KeyEvent evt) {                                        
        char e = evt.getKeyChar();
        if (!Character.isDigit(e) && e != '.') {
            evt.consume();
        }
        
        if (e == '.' && commisionField.getText().contains(".")) {
            evt.consume();
        }
    }                                       

    private void commisionFieldKeyPressed(java.awt.event.KeyEvent evt) {                                          

    }                                         

    private void commisionFieldKeyReleased(java.awt.event.KeyEvent evt) {                                           
        calculateAmount();
    }                                          
    
    private void setTitleVoucher() {
        if (jRadioButton4.isSelected()) {
            titleVoucherField.setText("KNIT CREATIVE INTERNATIONAL, S.A");
        }
        
        if (jRadioButton5.isSelected()) {
            titleVoucherField.setText("APPAREL KCI, S.A");
        }
    }
    
    private boolean validateFields() {
        if (ValidateField.isEmpty(titleVoucherField) && ValidateField.isEmpty(amountField)
                && voucherTableDescription.getRowCount() > 0) {
            return true;
        }
        return false;
    }
    
    private void calculateAmount() {
        double amount = 0.00;
        double gTotal = 0.00;
        double comission = 0.00;
        
        if (voucherTableDescription.getRowCount() > 0) {
            for (int i = 0; i < voucherTableDescription.getRowCount(); i++) {
                amount += ((BigDecimal) voucherTableDescription.getValueAt(i, 3)).doubleValue();
                
            }
            
            if (!commisionField.getText().isEmpty()) {
                comission = Double.parseDouble(commisionField.getText());
            }
            
            if (radioDollars.isSelected()) {
                Currency currency = Currency.getInstance("USD");
                DecimalFormat currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance();
                currencyFormat.setCurrency(currency);
                
                String formattedValue = currencyFormat.format(amount);
                
                amountField.setText(formattedValue);
                formattedValue = currencyFormat.format((amount + comission));
                gTotalField.setText(formattedValue);
                
            }
            
            if (radioQuetzales.isSelected()) {
                Currency currency = Currency.getInstance("GTQ");
                DecimalFormat currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance();
                currencyFormat.setCurrency(currency);
                
                String formattedSuccessFulValue = currencyFormat.format(amount);
                amountField.setText(formattedSuccessFulValue);
                formattedSuccessFulValue = currencyFormat.format((amount + comission));
                gTotalField.setText(formattedSuccessFulValue);
                
            }
        } else {
            amountField.setText("");
            commisionField.setText("");
        }
        
    }

    // Variables declaration - do not modify                     
    private javax.swing.JPanel amount;
    private javax.swing.JFormattedTextField amountField;
    private javax.swing.JRadioButton cash;
    private javax.swing.JRadioButton cashierCheck;
    private javax.swing.JLabel comissionLabel;
    private javax.swing.JFormattedTextField commisionField;
    private javax.swing.JTextField companyField;
    private javax.swing.JRadioButton creditCard;
    private javax.swing.ButtonGroup currency;
    private javax.swing.JRadioButton detailedVoucherRBtn;
    private javax.swing.JFormattedTextField gTotalField;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private com.toedter.calendar.JDateChooser paymentDateChooser;
    private javax.swing.ButtonGroup paymentMethod;
    public static javax.swing.JRadioButton radioDollars;
    public static javax.swing.JRadioButton radioQuetzales;
    private javax.swing.JTextArea remarks;
    private javax.swing.JMenuItem remove;
    private javax.swing.JPopupMenu removePurchaseList;
    private javax.swing.JComboBox signaturesGroupCB;
    private javax.swing.JRadioButton simpleVoucherRBtn;
    public static javax.swing.JTable tablePurchasesAvalaible;
    private javax.swing.ButtonGroup titleGroupButton;
    private javax.swing.JTextField titleVoucherField;
    private javax.swing.ButtonGroup typeVoucherGroup;
    private javax.swing.JTextField userName;
    public static javax.swing.JTable voucherTableDescription;
    // End of variables declaration                   

}
