/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.ui;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.text.Element;

/**
 * 
 * @author od
 */
public class JConsolePanel extends JTextArea {

    private static final long serialVersionUID = 1L;

    class DefaultCommandHandler implements CommandHandler {

        @Override
        public String handle(String cmd) {
            if (cmd.equals("clear")) {
                setText("");
            } else {
                return "unknown command '" + cmd + "'\n";
            }
            return null;
        }
    }

    static class Prompt {

        public String getPrompt() {
            return "> ";
        }
    }

    static class History {

        List<String> cmds = new ArrayList<String>();
        int cursor;

        public String next() {
            if (cmds.size() == 0) {
                return null;
            }
            if (cursor < cmds.size() - 1) {
                cursor++;
            }
            return cmds.get(cursor);
        }

        public String prev() {
            if (cmds.size() == 0) {
                return null;
            }
            if (cursor > 0) {
                cursor--;
            }
            return cmds.get(cursor);
        }

        public void appendCommand(String command) {
            cmds.add(command);
            cursor = cmds.size();
        }
    }
//
    private PrintWriter w = new PrintWriter(new Writer() {

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            JConsolePanel.this.append(new String(cbuf, off, len));
            setCaretPosition(getText().length());
            cp = getCaretPosition();
            JConsolePanel.this.scrollRectToVisible(new Rectangle(0, getHeight(), 1, 1));
        }
    });
//
    CommandHandler cmd;
    Prompt prompt;
    History history;
    int cp;

    public JConsolePanel(Prompt p, CommandHandler cmd, History hist) {
        setPromptProvider(p);
        setCommandHandler(cmd);
        setHistory(hist);

        setText(prompt.getPrompt());
        cp = prompt.getPrompt().length();

        setEditable(true);
        setLineWrap(true);
//        setRows(3);
//        setColumns(20);
     
        addKeyListener(new KListener());
        setCaretPosition(getText().length());
        setFont(new Font("Monospaced", 1, 12));
        addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                //TODO better caret handling after selection
                //TODO copy/paste ala shell
//                String sel = getSelectedText();
                int caretPosition = getCaretPosition();
                Element el = getDocument().getDefaultRootElement();
                int msel = el.getElementIndex(caretPosition) + 1;
                int last = el.getElementIndex(getText().length()) + 1;
                if (msel != last) {
                    setCaretPosition(getText().length());
                }
            }

        });
    }

    public JConsolePanel() {
        this(new Prompt(), null, new History());
        setCommandHandler(new DefaultCommandHandler());
    }

    public OutputStream getOutputStream() {
        return new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                JConsolePanel.this.append(Character.toString((char) b));
                setCaretPosition(getText().length());
                cp = getCaretPosition();
                JConsolePanel.this.scrollRectToVisible(new Rectangle(0, getHeight(), 1, 1));
            }
        };
    }

    public void prompt() {
        JConsolePanel.this.append(prompt.getPrompt());
        setCaretPosition(getText().length());
        cp = getCaretPosition();
        JConsolePanel.this.scrollRectToVisible(new Rectangle(0, getHeight(), 1, 1));
    }

    public void clear() {
        setText("");
        cp = prompt.getPrompt().length();
    }

    //TODO cascading commandhandler (as List, processed from bottom to top)
    public void setCommandHandler(CommandHandler h) {
        cmd = h;
    }

    public void setPromptProvider(Prompt p) {
        prompt = p;
    }

    public void setHistory(History h) {
        history = h;
    }

    public PrintWriter getOut() {
        return w;
    }

    private class KListener extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent key) {
            switch (key.getKeyCode()) {
                case KeyEvent.VK_C:
                    if (key.isControlDown()) {
                        key.consume();
                        append("\n");
                        append(prompt.getPrompt());
                        setCaretPosition(getText().length());
                        cp = getCaretPosition();
                    }
                    break;
                case KeyEvent.VK_UP:
                    String prev = history.prev();
                    if (prev != null) {
                        replaceRange(prev, cp, getText().length());
                    }
                    key.consume();
                    break;
                case KeyEvent.VK_DOWN:
                    String next = history.next();
                    if (next != null) {
                        replaceRange(next, cp, getText().length());
                    }
                    key.consume();
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_BACK_SPACE:
                    if (cp == getCaretPosition()) {
                        key.consume();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    key.consume();
                    String t = getText();
                    append("\n");
                    if (cmd != null) {
                        String c = t.substring(cp);
                        if (!c.isEmpty()) {
                            append(cmd.handle(c));
                            history.appendCommand(c);
                        }
                    }
                    append(prompt.getPrompt());
                    setCaretPosition(getText().length());
                    cp = getCaretPosition();
            }
        }
    }

    public static void main(String[] args) throws Exception {
//        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JConsolePanel cp = new JConsolePanel();
        cp.getOut().println("test hetre");
        cp.getOut().println("test next");
        cp.prompt();

        JScrollPane sp = new JScrollPane(cp);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        f.getContentPane().add(sp);
        f.setSize(500, 500);
        f.setLocation(300, 300);
        f.setVisible(true);
    }
}
