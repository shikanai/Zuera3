package llvmast;
public  class LlvmIcmp extends LlvmInstruction{
	public LlvmRegister lhs;
	public int conditionCode;
    public LlvmType type;
    public LlvmValue op1, op2;
    
    public LlvmIcmp(LlvmRegister lhs,  int conditionCode, LlvmType type, LlvmValue op1, LlvmValue op2){
    	this.lhs = lhs;
    	this.conditionCode = conditionCode;
    	this.type = type;
    	this.op1 = op1;
    	this.op2 = op2;
    }

    public String toString(){
    	//slt -> lessthan
    	if(conditionCode == 0){
    		return "  " + lhs + " = icmp " + "slt" + " " + type + " " + op1 + ", " + op2;
    	}
    	//eq -> equal
    	if(conditionCode == 1){
    		return "  " + lhs + " = icmp " + "eq" + " " + type + " " + op1 + ", " + op2;
    	}
		return null;
    }
    
    /*
    eq: equal
	ne: not equal
	ugt: unsigned greater than
	uge: unsigned greater or equal
	ult: unsigned less than
	ule: unsigned less or equal
	sgt: signed greater than
	sge: signed greater or equal
	slt: signed less than
	sle: signed less or equal*/
}