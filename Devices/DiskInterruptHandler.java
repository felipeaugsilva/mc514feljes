package osp.Devices;
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

/**
    The disk interrupt handler.  When a disk I/O interrupt occurs,
    this class is called upon the handle the interrupt.

    @OSPProject Devices
*/
public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    /** 
        Handles disk interrupts. 
        
        This method obtains the interrupt parameters from the 
        interrupt vector. The parameters are IORB that caused the 
        interrupt: (IORB)InterruptVector.getEvent(), 
        and thread that initiated the I/O operation: 
        InterruptVector.getThread().
        The IORB object contains references to the memory page 
        and open file object that participated in the I/O.
        
        The method must unlock the page, set its IORB field to null,
        and decrement the file's IORB count.
        
        The method must set the frame as dirty if it was memory write 
        (but not, if it was a swap-in, check whether the device was 
        SwapDevice)

        As the last thing, all threads that were waiting for this 
        event to finish, must be resumed.

        @OSPProject Devices 
    */
    public void do_handleInterrupt()
    {
        IORB iorb, next_iorb;
        Event event;
        ThreadCB thread;
        OpenFile openfile;
        Device device;
        PageTableEntry page;

        iorb = (IORB)InterruptVector.getEvent();
        thread = iorb.getThread();
        page = iorb.getPage();
        device = Device.get(iorb.getDeviceID());

        openfile = iorb.getOpenFile();
        openfile.decrementIORBCount();

        if(openfile.closePending && openfile.getIORBCount() == 0) openfile.close();       // fecha o arquivo se for o caso

        page.unlock();        
       
        if(iorb.getDeviceID() != SwapDeviceID)                    //verifica se o device é o SwapDevice
        {
            if(thread.getTask().getStatus() != TaskTerm){
                if(thread.getStatus() != ThreadKill)
                {
                        page.getFrame().setReferenced(true);
                        if(iorb.getIOType() == FileRead) page.getFrame().setDirty(true);
                }
            }
        }
        else{
            if(thread.getTask().getStatus() != TaskTerm){
                if(thread.getStatus() != ThreadKill){
                    page.getFrame().setDirty(false);
                }
            }
        }
            
       
        if(thread.getTask().getStatus() == TaskTerm) {
            if(page.getFrame().getReserved() == page.getTask())
                    page.getFrame().setUnreserved(page.getTask());                
        }

        iorb.notifyThreads();
        device.setBusy(false);                             // device esta idle
        next_iorb = device.dequeueIORB();                  //pega proximo I/O

        if(next_iorb != null) device.startIO(next_iorb);

        ThreadCB.dispatch();
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
