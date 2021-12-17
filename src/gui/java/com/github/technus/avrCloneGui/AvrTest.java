package com.github.technus.avrCloneGui;

import com.bulenkov.darcula.ui.DarculaTabbedPaneUI;
import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.compiler.Binding;
import com.github.technus.avrClone.compiler.IIncludeProcessor;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.exceptions.InvalidInclude;
import com.github.technus.avrClone.compiler.js.CompilerBindings;
import com.github.technus.avrClone.instructions.ExecutionEvent;
import com.github.technus.avrClone.instructions.IInstruction;
import com.github.technus.avrClone.instructions.InstructionRegistry;
import com.github.technus.avrClone.instructions.OperandLimit;
import com.github.technus.avrClone.memory.program.ProgramMemory;
import com.github.technus.avrCloneGui.Editors.IntegerEditor;
import com.github.technus.avrCloneGui.dataMemory.IRefreshDataMemoryView;
import com.github.technus.avrCloneGui.dataMemory.cpuTable.CpuTableModel;
import com.github.technus.avrCloneGui.dataMemory.cpuTable.CpuTablePopup;
import com.github.technus.avrCloneGui.dataMemory.dataTable.DataTableModel;
import com.github.technus.avrCloneGui.dataMemory.dataTable.DataTablePopup;
import com.github.technus.avrCloneGui.presentation.PresentationCellRenderer;
import com.github.technus.avrCloneGui.presentation.Presentations;
import com.github.technus.avrCloneGui.programMemory.programTable.ProgramTableModel;
import com.github.technus.avrCloneGui.programMemory.programTable.ProgramTablePopup;
import com.github.technus.avrCloneGui.registerFile.registerTable.RegisterTableModel;
import com.github.technus.avrCloneGui.registerFile.registerTable.RegisterTablePopup;
import jsyntaxpane.syntaxkits.AsmSyntaxKit;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static com.github.technus.avrClone.compiler.IIncludeProcessor.*;

public class AvrTest {
    private JEditorPane asm;
    private AsmSyntaxKit kit;

    private JTable program;
    private ProgramTableModel programModel;
    private PresentationCellRenderer programRenderer;

    private JTable registers;
    private RegisterTableModel registersModel;
    private PresentationCellRenderer registersRenderer;

    private JTable cpuStatus;
    private CpuTableModel cpuStatusModel;
    private PresentationCellRenderer cpuStatusRenderer;

    private JTable data;
    private DataTableModel dataModel;
    private PresentationCellRenderer dataRenderer;

    private JButton resetButton;
    private JButton stepButton;
    private JButton runButton;

    private JSpinner programCounterSpinner;

    private JPanel mainPanel;

    private JComboBox<InstructionRegistry> registry;
    private JTextPane instructions;
    private JTextPane limits;
    private JTabbedPane tabbedPane1;
    private JTextPane csegData;
    private JTextPane dsegData;
    private JTextPane esegData;

    public final AvrCore core;
    public final ProgramCompiler programCompiler;

    private final ArrayList<IRefreshDataMemoryView> dataTables=new ArrayList<>();
    private SpinnerNumberModel pcSpinner=new SpinnerNumberModel();

    private Thread runner,compiler;

    private String path=".";

    public AvrTest(AvrCore core,ProgramCompiler programCompiler) {
        this.core=core;
        this.programCompiler=programCompiler;
        IIncludeProcessor DIRECT_INCLUDE_PROCESSOR = (parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths) -> {
            if (includeName.startsWith("\"") && includeName.endsWith("\"")) {
                includeName = includeName.replaceFirst("^\"(.*)\"$", "$1");
                try {
                    return RELATIVE_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
                } catch (InvalidInclude e) {
                    return GLOBAL_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
                }
            } else if (includeName.startsWith("<") && includeName.endsWith(">")) {
                includeName = includeName.replaceFirst("^<(.*)>$", "$1");
                return SYSTEM_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
            } else if (parentIncludePath.length() == 0) {
                try {
                    if (path.equals(includeName)) {
                        ArrayList<String> lines = new ArrayList<>(Arrays.asList(asm.getText().split("\\r?\\n")));
                        includedFilePaths.put(includePath, path);
                        return lines;
                    } else {
                        return ABSOLUTE_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
                    }
                } catch (Exception e) {
                    throw new InvalidInclude("Failed to read file! " + includeName, e);
                }
            }
            throw new InvalidInclude("Invalid inclusion type! " + includeName);
        };
        this.programCompiler.sources.setCurrentIncludeProcessor(DIRECT_INCLUDE_PROCESSOR);

        tabbedPane1.setUI(new DarculaTabbedPaneUI() {
            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex,
                                             int fontHeight) {
                return 21;
            }
        });

        pcSpinner.addChangeListener(e -> {
            Object v=programCounterSpinner.getValue();
            if(v instanceof Number) {
                core.programCounter = ((Number)v).intValue();
            }
        });
        programCounterSpinner.setModel(pcSpinner);

        registers.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        registers.setModel(registersModel=new RegisterTableModel(core));
        registers.setDefaultEditor(Integer.class,new IntegerEditor(Integer.MIN_VALUE,Integer.MAX_VALUE));
        registers.setDefaultRenderer(Integer.class,registersRenderer=new PresentationCellRenderer(registers.getColumnCount(),Presentations.INT_DEC));
        registers.setComponentPopupMenu(new RegisterTablePopup(this,registers,registersModel,registersRenderer));
        registers.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));

        data.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        data.setModel(dataModel=new DataTableModel(core,dataTables));
        data.setDefaultEditor(Integer.class,new IntegerEditor(Integer.MIN_VALUE,Integer.MAX_VALUE));
        data.setDefaultRenderer(Integer.class,dataRenderer=new PresentationCellRenderer(data.getColumnCount(),Presentations.INT_DEC));
        data.setComponentPopupMenu(new DataTablePopup(this,data,dataModel,dataRenderer));
        data.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        dataTables.add(dataModel);

        cpuStatus.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        cpuStatus.setModel(cpuStatusModel=new CpuTableModel(core,dataTables));
        cpuStatus.setDefaultEditor(Integer.class,new IntegerEditor(Integer.MIN_VALUE,Integer.MAX_VALUE));
        cpuStatus.setDefaultRenderer(Integer.class,cpuStatusRenderer=new PresentationCellRenderer(cpuStatus.getColumnCount(),Presentations.INT_DEC));
        cpuStatus.setComponentPopupMenu(new CpuTablePopup(this,cpuStatus,cpuStatusModel,cpuStatusRenderer));
        cpuStatus.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        dataTables.add(cpuStatusModel);

        program.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        program.setModel(programModel=new ProgramTableModel(core));
        program.setDefaultEditor(Integer.class,new IntegerEditor(Integer.MIN_VALUE,Integer.MAX_VALUE));
        program.setDefaultRenderer(Integer.class,programRenderer=new PresentationCellRenderer(program.getColumnCount(),Presentations.INT_DEC));
        program.setComponentPopupMenu(new ProgramTablePopup(this,program,programModel,programRenderer));
        program.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));

        stepButton.addActionListener(e -> {
            try {
                ExecutionEvent event = core.cycle();
                refreshRegistersDataPc();
                if (event != null) {
                    JOptionPane.showMessageDialog(stepButton, event, "Execution event "+event.instruction.name()+"!", JOptionPane.INFORMATION_MESSAGE);
                }
            }catch (Exception ex){
                JOptionPane.showMessageDialog(stepButton, scrollThrowable(ex), "Execution thrown "+ex.getClass().getSimpleName()+"!", JOptionPane.ERROR_MESSAGE);
            }
        });

        runButton.addActionListener(e -> {
            if(runner==null){
                runner=new Thread(() -> {
                    ExecutionEvent event;
                    Long time=System.currentTimeMillis();
                    try {
                        while ((event = core.cycle()) == null && !Thread.currentThread().isInterrupted()) {
                            if(time+1000<System.currentTimeMillis()) {
                                time+=1000;
                                refreshRegistersDataPc();
                            }
                        }
                        if (event != null) {
                            JOptionPane.showMessageDialog(runButton, event, "Execution event "+event.instruction.name()+"!", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }catch (Exception ex){
                        JOptionPane.showMessageDialog(runButton, scrollThrowable(ex), "Execution thrown "+ex.getClass().getSimpleName()+"!", JOptionPane.ERROR_MESSAGE);
                    }
                    refreshRegistersDataPc();
                    runButton.setText("Start");
                    runner=null;
                });
                runner.start();
                runButton.setText("Stop");
            }else{
                try {
                    runner.interrupt();
                }catch (Exception ignored){}
            }
        });

        resetButton.addActionListener(e -> {
            core.reset();
            refreshRegistersDataPc();
        });

        asm.setEditorKit(kit=new AsmSyntaxKit());


        JMenuItem item=new JMenuItem("Compile ASM");
        asm.getComponentPopupMenu().add(new JPopupMenu.Separator());
        item.addActionListener(e -> compile());
        asm.getComponentPopupMenu().add(item);
        item=new JMenuItem("Load");
        item.addActionListener(e -> {
            JFileChooser jfc=new JFileChooser(path);
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setMultiSelectionEnabled(false);
            jfc.showOpenDialog(asm);
            File f=jfc.getSelectedFile();
            path=f.getAbsolutePath();
            try {
                asm.setText(new String(Files.readAllBytes(f.toPath())));
            }catch (Exception ex){
                JOptionPane.showMessageDialog(asm, scrollThrowable(ex), "Compiler thrown "+ex.getClass().getSimpleName()+"!", JOptionPane.ERROR_MESSAGE);
            }
        });
        asm.getComponentPopupMenu().add(item);
        item=new JMenuItem("Save");
        item.addActionListener(e -> {
            if(path!=null) {
                File f = new File(path);
                if(!f.isFile()){
                    return;
                }
                try {
                    Files.write(f.toPath(), asm.getText().getBytes());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(asm, scrollThrowable(ex), "Compiler thrown " + ex.getClass().getSimpleName() + "!", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        asm.getComponentPopupMenu().add(item);
        item=new JMenuItem("Save as");
        item.addActionListener(e -> {
            JFileChooser jfc=new JFileChooser(path);
            jfc.showSaveDialog(asm);
            try {
                Files.write(jfc.getSelectedFile().toPath(), asm.getText().getBytes());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(asm, scrollThrowable(ex), "Compiler thrown " + ex.getClass().getSimpleName() + "!", JOptionPane.ERROR_MESSAGE);
            }
        });
        asm.getComponentPopupMenu().add(item);

        asm.setSelectionColor(new Color(0x214283));

        if(core.getProgramMemory()!=null){
            asm.setText(core.getProgramMemory().getProgram(10));
        }

        InstructionRegistry.REGISTRIES.forEach((k,v)->registry.addItem(v));
        registry.addActionListener(e -> refreshWithInstructionRegistry(registry.getItemAt(registry.getSelectedIndex())));
        registry.setSelectedItem(core.getInstructionRegistry());

        refreshRegistersDataPc();
        refreshWithInstructionRegistry(core.getInstructionRegistry());
        refreshLimitsRegistry();
        refreshProgramMemory();
    }

    public void compile(){
        if(compiler!=null){
            compiler.interrupt();
        }
        compiler=new Thread(()->{
            CompilerBindings compilerBindings=new CompilerBindings(64);
            try {
                for(Map.Entry<String,Integer> entry: core.getDataNames().entrySet()){
                    compilerBindings.putBinding(entry.getKey(),new Binding(Binding.NameType.EQU,entry.getValue()));
                }
                for(Map.Entry<String,Integer> entry: core.getRegisterNames().entrySet()){
                    compilerBindings.putBinding(entry.getKey(),new Binding(Binding.NameType.EQU,entry.getValue()));
                }
            }catch (Exception ex){
                JOptionPane.showMessageDialog(asm, scrollThrowable(ex), "Compiler thrown "+ex.getClass().getSimpleName()+"!", JOptionPane.ERROR_MESSAGE);
            }
            try {
                programCompiler.setCompilerBindings(compilerBindings);
                programCompiler.compile(path);

                StringBuilder stringBuilder=new StringBuilder();
                for(String s:programCompiler.getDataCSEG()){
                    stringBuilder.append(s).append("\n");
                }
                csegData.setText(stringBuilder.toString());

                stringBuilder=new StringBuilder();
                for(String s:programCompiler.getDataDSEG()){
                    stringBuilder.append(s).append("\n");
                }
                dsegData.setText(stringBuilder.toString());

                stringBuilder=new StringBuilder();
                for(String s:programCompiler.getDataESEG()){
                    stringBuilder.append(s).append("\n");
                }
                esegData.setText(stringBuilder.toString());

                stringBuilder=new StringBuilder();
                for(String s:programCompiler.getProgram()){
                    stringBuilder.append(s).append("\n");
                }
                String program=stringBuilder.toString();
                JOptionPane.showMessageDialog(asm,scrollable(program));
                core.setProgramMemory(new ProgramMemory(core.getInstructionRegistry(),core.isUsingImmersiveOperands(), program.split("\\n")));

                refreshProgramMemory();
            } catch (Exception ex){
                JOptionPane.showMessageDialog(asm, scrollThrowable(ex), "Compiler thrown "+ex.getClass().getSimpleName()+"!", JOptionPane.ERROR_MESSAGE);
            }

        });
        compiler.start();
    }

    public JFrame show(){
        JFrame frame=new JFrame();
        frame.setContentPane(getMainPanel());
        frame.pack();
        frame.setTitle("AVR TEST GUI");
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        return frame;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void refreshRegistersDataPc(){
        for (IRefreshDataMemoryView refreshDataMemoryView:dataTables) {
            refreshDataMemoryView.refreshDataMemoryView();
        }
        registersModel.refreshRegisterMemoryView();
        pcSpinner.setValue(core.programCounter);
    }

    public void refreshProgramMemory(){
        programModel.refreshProgramMemoryView();
    }

    public void refreshWithInstructionRegistry(InstructionRegistry registry){
        if(core.getInstructionRegistry()!=null){
            StringBuilder stringBuilder=new StringBuilder();
            for(IInstruction instruction:registry.getInstructions()){
                switch (instruction.getOperandCount()){
                    case 0:
                        stringBuilder.append(String.format("%1$-20s",instruction.name()));
                        break;
                    case 1:
                        stringBuilder.append(String.format("%1$-8s",instruction.name()));
                        stringBuilder.append(String.format("%1$-12s",instruction.getLimit0().name));
                        break;
                    case 2:
                        stringBuilder.append(String.format("%1$-8s",instruction.name()));
                        stringBuilder.append(String.format("%1$-6s",instruction.getLimit0().name));
                        stringBuilder.append(String.format("%1$-6s",instruction.getLimit1().name));
                        break;
                }
                stringBuilder.append(instruction.getNotes()).append("\n");
            }
            instructions.setText(stringBuilder.toString());
        }
    }

    public void refreshLimitsRegistry(){
        StringBuilder stringBuilder=new StringBuilder();
        for(OperandLimit limit:OperandLimit.registry){
            if(limit.getBroader()!=null){
                stringBuilder.append(String.format("%1$-6s",limit.name)).append(String.format("(%1$-6s) ",limit.getBroader().name));
            }else {
                stringBuilder.append(String.format("%1$-15s",limit.name));
            }
            stringBuilder.append(limit.getPossibleValuesString()).append("\n");
        }
        limits.setText(stringBuilder.toString());
    }

    public static String printThrowable(Throwable t){
        OutputStream outputStream=new ByteArrayOutputStream();
        PrintStream printStream=new PrintStream(outputStream);
        t.printStackTrace(printStream);
        t.printStackTrace(System.err);
        try {
            printStream.flush();
            outputStream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
        String trace=new String(((ByteArrayOutputStream) outputStream).toByteArray());
        try {
            printStream.close();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return trace;
    }

    public static JScrollPane scrollThrowable(Throwable t){
        return scrollable(printThrowable(t));
    }

    public static JScrollPane scrollable(String t){
        JTextArea area=new JTextArea();
        area.setEditable(false);
        area.setText(t);
        JScrollPane pane=new JScrollPane(area);
        pane.setPreferredSize(new Dimension(700,500));
        return pane;
    }

    public void writeASM(String s){
        asm.setText(s);
    }
}
