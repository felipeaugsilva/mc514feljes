
/*
* Grupo 05
* RA: 081704
* RA: 096993
*
* Status:
*
* 02/09/2010
* 1. Compila porém tem erros na simulação.

*
* */ 

package osp.Tasks;

import java.util.Enumeration;
import java.util.Vector;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**
    The student module dealing with the creation and killing of
    tasks.  A task acts primarily as a container for threads and as
    a holder of resources.  Execution is associated entirely with
    threads.  The primary methods that the student will implement
    are do_create(TaskCB) and do_kill(TaskCB).  The student can choose
    how to keep track of which threads are part of a task.  In this
    implementation, an array is used.

    @OSPProject Tasks
*/
public class TaskCB extends IflTaskCB
{
	private GenericList portsList;
	private GenericList filesList;
	private GenericList threadsList;
	
    /**
       The task constructor. Must have

       	   super();

       as its first statement.

       @OSPProject Tasks
    */
    public TaskCB()
    {
        super();

        //Criacao das listas do task.
    	threadsList = new GenericList();
    	portsList = new GenericList();
    	filesList = new GenericList();

        //dados do task
    	this.setCreationTime(HClock.get());
    	this.setStatus(TaskLive);
    	this.setPriority(0);         
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Tasks
    */
    public static void init()
    {

    }

    /** 
        Sets the properties of a new task, passed as an argument. 
        
        Creates a new thread list, sets TaskLive status and creation time,
        creates and opens the task's swap file of the size equal to the size
	(in bytes) of the addressable virtual memory.

	@return task or null

        @OSPProject Tasks
    */
    static public TaskCB do_create()
    {    	
        //criacao do task
        TaskCB task = new TaskCB();
  
        //tabela com a pagina do task.
    	PageTable pag = new PageTable(task);
    	task.setPageTable(pag);

       //criacao e definicao do arquivo de swap desta task
        String nome = SwapDeviceMountPoint+task.getID();
        FileSys.create(nome, (int)Math.pow(2, MMU.getVirtualAddressBits()));
        OpenFile swap = OpenFile.open(nome, task);
        
        //se nao houver espaco para a criacao do arquivo entao ira despachar uma thread nova e retornar null
        if(swap == null){
          ThreadCB nova;
          nova = ThreadCB.create(task);
          nova.dispatch();
          return null;
        }
        else task.setSwapFile(swap);

    	//criacao da primeira thread
        ThreadCB thread = ThreadCB.create(task);

        return task;
    }

    /**
       Kills the specified task and all of it threads. 

       Sets the status TaskTerm, frees all memory frames 
       (reserved frames may not be unreserved, but must be marked 
       free), deletes the task's swap file.
	
       @OSPProject Tasks
    */
    public void do_kill()
    {
        Enumeration num;
		
		//Remover threads
		num = threadsList.forwardIterator();
		while(num.hasMoreElements()) {
			ThreadCB thread = (ThreadCB)num.nextElement();
			thread.kill();
		}
		
		//Remover portas
		num = portsList.forwardIterator();
		while(num.hasMoreElements()) {
			PortCB port = (PortCB)num.nextElement();
			port.destroy();
		}
		
		//Mudar status pra terminado
		this.setStatus(TaskTerm);
		
		//Desalocar memoria
		PageTable pag = this.getPageTable();
        pag.deallocateMemory();
		
		//Fechar arquivos
		num = filesList.forwardIterator();
		while(num.hasMoreElements()) {
			OpenFile file = (OpenFile)num.nextElement();
			file.close();
		}
		
		//Destruir arquivo de swap
        FileSys.delete(SwapDeviceMountPoint+this.getID());
    }

    /** 
	Returns a count of the number of threads in this task. 
	
	@OSPProject Tasks
    */
    public int do_getThreadCount()
    {
    	return threadsList.length();
    }

    /**
       Adds the specified thread to this task. 
       @return FAILURE, if the number of threads exceeds MaxThreadsPerTask;
       SUCCESS otherwise.
       
       @OSPProject Tasks
    */
    public int do_addThread(ThreadCB thread)
    {
        //Verifica se a task ja nao tem o maximo de threads
		if(this.do_getThreadCount() < ThreadCB.MaxThreadsPerTask) {
			threadsList.insert(thread);
			return SUCCESS;
		}
		return FAILURE;
    }

    /**
       Removes the specified thread from this task. 		

       @OSPProject Tasks
    */
    public int do_removeThread(ThreadCB thread)
    {
        if(threadsList.remove(thread) == null)
			return FAILURE;
		return SUCCESS;
    }

    /**
       Return number of ports currently owned by this task. 

       @OSPProject Tasks
    */
    public int do_getPortCount()
    {
        return portsList.length();
    }

    /**
       Add the port to the list of ports owned by this task.
	
       @OSPProject Tasks 
    */ 
    public int do_addPort(PortCB newPort)
    {
        //Verifica se a task ja nao tem o maximo de portas associadas a ela
		if(this.do_getPortCount() < PortCB.MaxPortsPerTask) {
			portsList.insert(newPort);
			return SUCCESS;
		}
		return FAILURE;
    }

    /**
       Remove the port from the list of ports owned by this task.

       @OSPProject Tasks 
    */ 
    public int do_removePort(PortCB oldPort)
    {
        if(portsList.remove(oldPort) == null)
			return FAILURE;
		return SUCCESS;
    }

    /**
       Insert file into the open files table of the task.

       @OSPProject Tasks
    */
    public void do_addFile(OpenFile file)
    {
        filesList.insert(file);
    }

    /** 
	Remove file from the task's open files table.

	@OSPProject Tasks
    */
    public int do_removeFile(OpenFile file)
    {
        if(filesList.remove(file) == null)
			return FAILURE;
		return SUCCESS;
    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures
       in their state just after the error happened.  The body can be
       left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atError()
    {
        // your code goes here

    }

    /**
       Called by OSP after printing a warning message. The student
       can insert code here to print various tables and data
       structures in their state just after the warning happened.
       The body can be left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
