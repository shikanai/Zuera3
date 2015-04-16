package llvmast;
public  class LlvmBranch extends LlvmInstruction{

    public LlvmValue cond;
	public LlvmLabelValue brTrue;
	public LlvmLabelValue brFalse;
	
    public LlvmBranch(LlvmLabelValue label){
    	this(null, label, null);
    }
    
    public LlvmBranch(LlvmValue cond,  LlvmLabelValue brTrue, LlvmLabelValue brFalse){
    	this.cond = cond;
    	this.brTrue = brTrue;
    	this.brFalse = brFalse;
    }
    
    //Falta esse método
    public String toString(){
    	if (cond == null){
    		return "  " + "br" + " label %" + brTrue;
    	}
    	//br i1 <cond>, label <iftrue>, label <iffalse>
    	return "  " + "br i1 " + cond + ", label %" + brTrue + "," + " label %" + brFalse;
    }
}
