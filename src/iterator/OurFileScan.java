package iterator;

import bufmgr.PageNotReadException;
import global.AttrType;
import global.RID;
import heap.*;
import iterator.*;

//import javax.swing.*;
import java.io.IOException;

public class OurFileScan extends Iterator{
    private AttrType[] _in1;
    private short in1_len;
    private short[] s_sizes;
    private Heapfile f;
    private Scan scan;
    private Tuple tuple1;
    private Tuple Jtuple;
    private int t1_size;
    private int nOutFlds;
    private CondExpr[] OutputFilter;
    private int[] _pref_list;
    public FldSpec[] perm_mat;

    /**
     * constructor
     *
     * @param file_name  heapfile to be opened
     * @param in1        array showing what the attributes of the input fields are.
     * @param s1_sizes   shows the length of the string fields.
     * @param len_in1    number of attributes in the input tuple
     * @param n_out_flds number of fields in the out tuple
     * @param proj_list  shows what input fields go where in the output tuple
     * @param outFilter  select expressions
     * @param pref_list  preference list to calculate sum
     * @throws IOException         some I/O fault
     * @throws FileScanException   exception from this class
     * @throws TupleUtilsException exception from this class
     * @throws InvalidRelation     invalid relation
     */
    public OurFileScan(String file_name, AttrType[] in1, short[] s1_sizes, short len_in1, int n_out_flds, FldSpec[] proj_list, CondExpr[] outFilter, int[] pref_list)
            throws IOException, FileScanException, TupleUtilsException, InvalidRelation {
        _in1 = in1;
        in1_len = len_in1;
        s_sizes = s1_sizes;
        _pref_list = pref_list;

        Jtuple = new Tuple();
        AttrType[] Jtypes = new AttrType[n_out_flds+1];
        short[] ts_size;
        ts_size = TupleUtils.setup_op_tuple(Jtuple, Jtypes, in1, len_in1, s1_sizes, proj_list, n_out_flds+1);

        OutputFilter = outFilter;
        perm_mat = proj_list;
        nOutFlds = n_out_flds+1;
        tuple1 = new Tuple();

        try {
            tuple1.setHdr(in1_len, _in1, s1_sizes);
        } catch (Exception e) {
            throw new FileScanException(e, "setHdr() failed");
        }
        t1_size = tuple1.size();

        try {
            f = new Heapfile(file_name);

        } catch (Exception e) {
            throw new FileScanException(e, "Create new heapfile failed");
        }

        try {
            scan = f.openScan();
        } catch (Exception e) {
            throw new FileScanException(e, "openScan() failed");
        }
    }
    /**
     * @return shows what input fields go where in the output tuple
     */
    public FldSpec[] show() {
        return perm_mat;
    }

    /**
     * @return the result tuple
     * @throws JoinsException                 some join exception
     * @throws IOException                    I/O errors
     * @throws InvalidTupleSizeException      invalid tuple size
     * @throws InvalidTypeException           tuple type not valid
     * @throws PageNotReadException           exception from lower layer
     * @throws PredEvalException              exception from PredEval class
     * @throws UnknowAttrType                 attribute type unknown
     * @throws FieldNumberOutOfBoundException array out of bounds
     * @throws WrongPermat                    exception for wrong FldSpec argument
     */
    public Tuple get_next()
            throws JoinsException,
            IOException,
            InvalidTupleSizeException,
            InvalidTypeException,
            PageNotReadException,
            PredEvalException,
            UnknowAttrType,
            FieldNumberOutOfBoundException,
            WrongPermat {
        RID rid = new RID();

        while (true) {
            if ((tuple1 = scan.getNext(rid)) == null) {
                return null;
            }

            tuple1.setHdr(in1_len, _in1, s_sizes);
            if (PredEval.Eval(OutputFilter, tuple1, null, _in1, null) == true) {
                Projection.Project(tuple1, _in1, Jtuple, perm_mat, nOutFlds);

                float sum = 0;
                for(int i=0 ; i<_pref_list.length;i++){
                    sum += Jtuple.getFloFld(_pref_list[i]);
//                    System.out.print(Jtuple.getFloFld(_pref_list[i]));
                }
                Jtuple.setFloFld(Jtuple.noOfFlds(), sum);
//                System.out.println(" | "+sum);
                return Jtuple;
            }
        }
    }

    /**
     * implement the abstract method close() from super class Iterator
     * to finish cleaning up
     */
    public void close() {

        if (!closeFlag) {
            scan.closescan();
            closeFlag = true;
        }
    }

}

