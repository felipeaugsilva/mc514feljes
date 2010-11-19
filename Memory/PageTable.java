package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        super(ownerTask);

        int numPages = (int)Math.pow(2, MMU.getPageAddressBits());

        pages = new PageTableEntry[numPages];

        for(int i = 0; i < numPages; i++)
            pages[i] = new PageTableEntry(this, i);
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
        FrameTableEntry frame;
        PageTableEntry page;

        // libera todos os frames que estavam associados a task
        for(int i = 0; i < (int)Math.pow(2, MMU.getPageAddressBits()); i++) {
            page = pages[i];

            frame = page.getFrame();
            if(frame != null) {
                frame.setDirty(false);
                frame.setReferenced(false);
                frame.setPage(null);
            }
        }

        // verifica se ha frames reservados para esta task
        for(int i = 0; i < MMU.getFrameTableSize(); i++) {
            frame = MMU.getFrame(i);
            if(frame.getReserved() == this.getTask())
                frame.setUnreserved(this.getTask());
        }

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
