/*****************************************************
Esta classe Codegen é a responsável por emitir LLVM-IR. 
Ela possui o mesmo método 'visit' sobrecarregado de
acordo com o tipo do parâmetro. Se o parâmentro for
do tipo 'While', o 'visit' emitirá código LLVM-IR que 
representa este comportamento. 
Alguns métodos 'visit' já estão prontos e, por isso,
a compilação do código abaixo já é possível.

class a{
    public static void main(String[] args){
    	System.out.println(1+2);
    }
}

O pacote 'llvmast' possui estruturas simples 
que auxiliam a geração de código em LLVM-IR. Quase todas 
as classes estão prontas; apenas as seguintes precisam ser 
implementadas: 

// llvmasm/LlvmBranch.java
// llvmasm/LlvmIcmp.java
// llvmasm/LlvmMinus.java
// llvmasm/LlvmTimes.java


Todas as assinaturas de métodos e construtores 
necessárias já estão lá. 


Observem todos os métodos e classes já implementados
e o manual do LLVM-IR (http://llvm.org/docs/LangRef.html) 
como guia no desenvolvimento deste projeto. 

****************************************************/
package llvm;

import semant.Env;
import syntaxtree.*;
import llvmast.*;

import java.util.*;

public class Codegen extends VisitorAdapter{
	private List<LlvmInstruction> assembler;
	private Codegen codeGenerator;

  	private SymTab symTab;
	private ClassNode classEnv; 	// Aponta para a classe atualmente em uso em symTab
	private MethodNode methodEnv; 	// Aponta para a metodo atualmente em uso em symTab
	
	public static int counter_label = 0;

	public Codegen(){
		assembler = new LinkedList<LlvmInstruction>();
		symTab = new SymTab();
	}

	// Método de entrada do Codegen
	public String translate(Program p, Env env){	
		codeGenerator = new Codegen();
		
		// Preenchendo a Tabela de Símbolos
		// Quem quiser usar 'env', apenas comente essa linha
		codeGenerator.symTab.FillTabSymbol(p);
		
		//System.out.format("***********terminando de preencher a SymTab...\n");
		
		// Formato da String para o //System.out.printlnijava "%d\n"
		codeGenerator.assembler.add(new LlvmConstantDeclaration("@.formatting.string", "private constant [4 x i8] c\"%d\\0A\\00\""));	

		// NOTA: sempre que X.accept(Y), então Y.visit(X);
		// NOTA: Logo, o comando abaixo irá chamar codeGenerator.visit(Program), linha 75
		p.accept(codeGenerator);

		// Link do printf
		List<LlvmType> pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		pts.add(LlvmPrimitiveType.DOTDOTDOT);
		codeGenerator.assembler.add(new LlvmExternalDeclaration("@printf", LlvmPrimitiveType.I32, pts)); 
		List<LlvmType> mallocpts = new LinkedList<LlvmType>();
		mallocpts.add(LlvmPrimitiveType.I32);
		codeGenerator.assembler.add(new LlvmExternalDeclaration("@malloc", new LlvmPointer(LlvmPrimitiveType.I8),mallocpts)); 


		String r = new String();
		for(LlvmInstruction instr : codeGenerator.assembler)
			r += instr+"\n";
		return r;
	}

	public LlvmValue visit(Program n){
		n.mainClass.accept(this);

		for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
			c.head.accept(this);

		return null;
	}

	
	
	
	public LlvmValue visit(MainClass n){
		
		// definicao do main 
		assembler.add(new LlvmDefine("@main", LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
		assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));
		LlvmRegister R1 = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		assembler.add(new LlvmAlloca(R1, LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
		assembler.add(new LlvmStore(new LlvmIntegerLiteral(0), R1));

		// Statement é uma classe abstrata
		// Portanto, o accept chamado é da classe que implementa Statement, por exemplo,  a classe "Print". 
		n.stm.accept(this);  

		// Final do Main
		LlvmRegister R2 = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmLoad(R2,R1));
		assembler.add(new LlvmRet(R2));
		assembler.add(new LlvmCloseDefinition());
		return null;
	}
	
	public LlvmValue visit(Plus n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmPlus(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;
	}
	
	public LlvmValue visit(Print n){

		LlvmValue v =  n.exp.accept(this);

		// getelementptr:
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
		LlvmRegister src = new LlvmNamedValue("@.formatting.string",new LlvmPointer(new LlvmArray(4,LlvmPrimitiveType.I8)));
		List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(new LlvmIntegerLiteral(0));
		offsets.add(new LlvmIntegerLiteral(0));
		List<LlvmType> pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		List<LlvmValue> args = new LinkedList<LlvmValue>();
		args.add(lhs);
		args.add(v);
		assembler.add(new LlvmGetElementPointer(lhs,src,offsets));

		pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		pts.add(LlvmPrimitiveType.DOTDOTDOT);
		
		// printf:
		assembler.add(new LlvmCall(new LlvmRegister(LlvmPrimitiveType.I32),
				LlvmPrimitiveType.I32,
				pts,				 
				"@printf",
				args
				));
		return null;
	}
	
	public LlvmValue visit(IntegerLiteral n){
		LlvmIntegerLiteral returns = new LlvmIntegerLiteral(n.value);
		return returns;
	};
	
	// Todos os visit's que devem ser implementados	
	public LlvmValue visit(ClassDeclSimple n){
		
		//recuperando classEnv do SymTab
		classEnv = symTab.classes.get(n.name.s);
		
		//System.out.format("****classEnv: \n%s \n%s \n%s \n%s\n",classEnv.classType, classEnv.nameClass, classEnv.type, classEnv.varList);
		
		//System.out.format("n: %s %s %s %s\n",n,n.name, n.methodList, n.varList);
		
		//seguindo o padrao do slide 21/47 parte 1 llvm...
		
		System.out.format("classdeclsimple*********************\n");
		
		int i, j;
		
		//criando uma string, onde sera colocado o assembly referente ao type. (type {...})
		StringBuilder classTypes = new StringBuilder();
		
		//criando uma string onde sera colocado o nome da classe
		StringBuilder className = new StringBuilder();
		
		//lista ligada onde serao armazenados os tipos das variaveis
		List<LlvmType> typeList = new LinkedList<LlvmType>();
		
		//%class.name
		className.append("%class.");
		className.append(n.name.s);
		
		//type { type1, type2, ... } - vamos utilizar LlvmStructure
		classTypes.append("type ");
		
		if (n.varList != null) {
			j = n.varList.size();
			//System.out.format("Numero de variaveis: %d\n", j);
			
			//itera a lista de variaveis para pegar todos os tipos e appendar em classTypes.
			for (util.List<VarDecl> varList = n.varList; varList != null; varList = varList.tail){
				
				LlvmValue variable_type = varList.head.type.accept(this);
				
				//System.out.format("tipos das variaveis:%s \n", variable_type);
				
				if(variable_type.toString().contains("%class")){
					//System.out.format("eh uma classe. alterando para pointer...\n");
					LlvmPointer ptr_class = new LlvmPointer((LlvmType) variable_type);
					typeList.add(ptr_class);
					
				}else{
					//adiciona os tipos de variaveis
					typeList.add((LlvmType) variable_type);
				}
		
			}
			
		}

		//Structure onde serao colocados os tipos, formatados pelo LlvmStructure
		LlvmStructure listaDeTipos = new LlvmStructure(typeList);
		
		if(listaDeTipos.toString().contains("null")){
			
			//System.out.format("listaDeTipos nula\n");
			
			//appenda a lista de tipos no classTypes
			classTypes.append("{ }");
			
		}
		else{
		
			//System.out.format("listaDeTipos nao nula\n");
			
			//appenda a lista de tipos no classTypes
			classTypes.append(listaDeTipos.toString());
			
		}
		System.out.format("\nclassType final: %s\n", classTypes);
		
		System.out.format("className: %s\n",className);
		
		// Adiciona declaracao de classe no assembly
		assembler.add(new LlvmConstantDeclaration(className.toString(),classTypes.toString()));

		//System.out.format("antes methodenv: %s\n", n.methodList);
			
		if(n.methodList != null) {
			j = n.methodList.size();
			//System.out.format("methodList.size: %s\n",n.methodList.size());
			
			
			for (util.List<MethodDecl> methodList = n.methodList; methodList != null; methodList = methodList.tail){
				MethodDecl method = methodList.head;
				
				System.out.format("@class - method: %s ", method);
				
				//desce para methods
				
				method.accept(this);
			}
			
		}
		
		return null;
		
	}
	
	//Heranca TODO
	public LlvmValue visit(ClassDeclExtends n){
		System.out.format("classdeclextends*********************\n");
		return null;
		
	}
	
	//Declaracao de variavel.
	public LlvmValue visit(VarDecl n){
		System.out.format("vardecl*********************\n");
		LlvmType varType = (LlvmType) n.type.accept(this);
		
		StringBuilder varDeclaration = new StringBuilder();
		
		//criando o nome do endereco onde sera alocada a variavel
		varDeclaration.append("%");
		varDeclaration.append(n.name.s);
		varDeclaration.append("_address");
		
		//System.out.format("var addr name: %s\n",varDeclaration.toString());
		
		//varType *
		LlvmPointer varTypePtr = new LlvmPointer(varType);
		
		//%name_address, varType *
		LlvmRegister registerVar = new LlvmRegister(varDeclaration.toString(), varTypePtr);
		
		//gera o assembly: %name_address = alloca type
		assembler.add(new LlvmAlloca(registerVar, varType, new LinkedList<LlvmValue>()));
		
		return registerVar;
		
	}
	
	//Metodos
	public LlvmValue visit(MethodDecl n){
		
		System.out.format("methoddecl*********************\n");
		//recuperando o methodEnv da symTab
		methodEnv = symTab.methods.get(n.name.s);
		//System.out.format("****methodEnv: \n%s \n%s \n%s \n%s\n",methodEnv.formals_name,methodEnv.formals_value,methodEnv.locals_name,methodEnv.locals_value);
						
		int i,j;
		
		LinkedList<LlvmValue> parametros = new LinkedList<LlvmValue>();
		LlvmType retType = (LlvmType) n.returnType.accept(this);
		StringBuilder declString = new StringBuilder();
		
		if(n.body!=null){
			//System.out.format("body.head: %s\n",n.body.head);
		}
		if(n.locals!=null){
			//System.out.format("locals: %s\n",n.locals.head);
		}
		//System.out.format("name: %s\n",n.name);
		//System.out.format("return type: %s\n",n.returnType);
		//System.out.format("return exp: %s\n",n.returnExp);
		
		//antes de sair preenchendo o formals, temos que colocar o %class * %this
		LlvmClassInfo class_this = new LlvmClassInfo(classEnv.nameClass);
		
		LlvmPointer class_this_ptr = new LlvmPointer(class_this);
		
		LlvmNamedValue this_val = new LlvmNamedValue("%this",class_this_ptr);
		
		//System.out.format("this_val: \n%s \n%s \n%s\n",this_val, this_val.name, this_val.type);
		
		parametros.add(this_val);
		
		//preenchendo a lista de parametros do metodo
		if(n.formals != null) {
			//System.out.format("formals: %s\n",n.formals.head);
			j = n.formals.size();
			//System.out.format("formals size: %d\n", j);
			
			for (util.List<Formal> formals = n.formals; formals != null; formals = formals.tail){
				LlvmValue param = formals.head.accept(this);
				System.out.format("formals: %s \n", formals.head);
				parametros.add(param);
			}
			
		}
		
		declString.append("@__");
		declString.append(n.name);
		
		//System.out.format("declString: %s\n",declString);
		//System.out.format("retType: %s\n",retType);
		
		//Se for retornar uma classe, retorna ponteiro para tipo da classe.
		if(retType.toString().contains("%class")){
			LlvmPointer ptr_retType = new LlvmPointer(retType);
			// Adiciona define de classe no assembly
			assembler.add(new LlvmDefine(declString.toString(), ptr_retType, parametros));
		}else{
			// Adiciona define de classe no assembly
			assembler.add(new LlvmDefine(declString.toString(), retType, parametros));
		}
		
		//Apos o define, devemos comecar a implementacao do metodo...
		
		//Copiando da main... Criando entrypoint
		assembler.add(new LlvmLabel(new LlvmLabelValue("entryMethod")));
		
		j = parametros.size();
		
		//Alocando memoria para todos os parametros, menos o referente a classe ( que eh o primeiro)
		for(i = 1; i < j ; i++){
			
			LlvmValue parametro_atual = parametros.get(i);
			
			//System.out.format("parametro_atual: %s\n",parametro_atual);
			
			//Cria ponteiro para o tipo, que sera utilizado como endereco de alocacao.
			LlvmPointer pointer_type = new LlvmPointer(parametro_atual.type);
			
			StringBuilder addr_name = new StringBuilder();
			
			addr_name.append(parametro_atual);
			addr_name.append("_address");
			
			//%name_address
			LlvmValue addr = new LlvmNamedValue(addr_name.toString(), pointer_type);
			
			//Aloca memoria para endereco
			assembler.add(new LlvmAlloca(addr, parametro_atual.type, new LinkedList<LlvmValue>()));
			
			//Armazena valor no endereco apontado por addr
			assembler.add(new LlvmStore(parametro_atual,addr));
		}

		
		//Itera todas as variaveis locais
		if(n.locals!=null){
			//System.out.format("locals size: %s\n",n.locals.size());
			j = n.locals.size();
			
			for (util.List<VarDecl> locals = n.locals; locals != null; locals = locals.tail){	
				//desce para varDecl para cada variavel local
				locals.head.accept(this);
				//System.out.format("locals****: %s \n", n.locals.head);
			}
		}
		
		
		//Itera nos statements do metodo
		if(n.body!=null){
			
			j = n.body.size();
			
			for (util.List<Statement> body = n.body; body != null; body = body.tail){
				System.out.format("body****: %s \n", body.head);
				//desce para stmt seguinte.
				body.head.accept(this);
			}
		}

		//retorno...
		assembler.add(new LlvmRet(n.returnExp.accept(this)));
		
		//}
		assembler.add(new LlvmCloseDefinition());
		
		return null;		
	}
	
	//Variavel de parametro do metodo
	public LlvmValue visit(Formal n){
	
		System.out.format("formal**********\n");
		
		StringBuilder name = new StringBuilder();
		
		name.append("%");
		name.append(n.name.s);
		
		//variable -> %name, com tipo n.type
		LlvmNamedValue variable = new LlvmNamedValue(name.toString(), (LlvmType)n.type.accept(this));
		
		return variable;		
	}
	
	public LlvmValue visit(IntArrayType n){
		System.out.format("intarraytype*************\n");
		
		//retorna novo tipo criado... Verificar se da certo haha :P
		return LlvmPrimitiveType.I32PTR;
	}
	
	//Como nao podemos retornar um LlvmType, extendemos LlvmType para LlvmValue
	public LlvmValue visit(BooleanType n){
		return LlvmPrimitiveType.I1;
	}
	
	//Como nao podemos retornar um LlvmType, extendemos LlvmType para LlvmValue
	public LlvmValue visit(IntegerType n){
		return LlvmPrimitiveType.I32;
	}
	
	//De forma alguma consegui pensar em como fazer isso sem adicionar uma nova classe
	//entao criei o LlvmClassInfo
	
	//So cai aqui quando eh class 
	public LlvmValue visit(IdentifierType n){
		System.out.format("identifiertype*******\n");
		
		//%class.name
		StringBuilder name = new StringBuilder();
		
		name.append(n.name);
		
		//System.out.format("name: %s\n",name.toString());
		
		//Cria classType -> %class.name
		LlvmClassInfo classType = new LlvmClassInfo(name.toString());	

		return classType;
	}
	
	//Na implementacao do block ,simplesmente iteramos o body inteiro do block
	//(dentro do while)	
	public LlvmValue visit(Block n){
		System.out.format("block****\n");
		
		//Verifica se existe algum elemento no block
		if(n.body != null){
			int i,j;
			
			j = n.body.size();
			//itera o block
			for (util.List<Statement> body = n.body; body != null; body = body.tail){
				
				//System.out.format("@block body: %s\n", n.body.head);
				//Desce para cada parte do block
				body.head.accept(this);
			}	
		}	
		return null;
	}
	
	//If
	public LlvmValue visit(If n){
		System.out.format("if*******\n");
		//Pega o registrador referente a condicao do if em cond
		LlvmValue cond = n.condition.accept(this);
		//System.out.format("cond: %s\n",cond);
		//System.out.format("n.condition: %s\n", n.condition);
		
		//Cria as labels referentes a cada branch. Contador incrementado a cada
		//label e concatenado no final faz com que a label seja unica
		LlvmLabelValue brTrue = new LlvmLabelValue("iflabeltrue"+counter_label);
		counter_label++;
		//System.out.format("label1: %s\n",brTrue);
		LlvmLabelValue brFalse = new LlvmLabelValue("iflabelfalse"+counter_label);
		counter_label++;
		//System.out.format("label2: %s\n",brFalse);
		//Cria label referente ao break da label para a qual pulamos.
		LlvmLabelValue brBreak = new LlvmLabelValue("iflabelbreak"+counter_label);
		counter_label++;
		//System.out.format("label3: %s\n",brBreak);
		
		if(n.elseClause!=null && n.thenClause!=null){
			//Faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brFalse se retornar 0
			assembler.add(new LlvmBranch(cond, brTrue, brFalse));

			//Label brTrue
			assembler.add(new LlvmLabel(brTrue));
			
			//Desce para thenClause
			n.thenClause.accept(this);

			//System.out.format("thenClause: %s\n",n.thenClause);

			//Se pulou para label brTrue, agora ele pula para o brBreak, com o intuito de pular o brFalse
			assembler.add(new LlvmBranch(brBreak));
			
			assembler.add(new LlvmLabel(brFalse));
			
			//System.out.format("elseClause: %s\n",n.elseClause);

			//Desce para elseClause
			n.elseClause.accept(this);
			
			assembler.add(new LlvmBranch(brBreak));
			
		} else if(n.elseClause!=null && n.thenClause==null){
			//Faz o branch condicional, pulando para label brBreak se a cond retornar 1, e para brFalse se retornar 0
			assembler.add(new LlvmBranch(cond, brBreak, brFalse));

			//System.out.format("elseClause != null");
		
			assembler.add(new LlvmLabel(brFalse));

			//System.out.format("elseClause: %s\n",n.elseClause);

			//Desce para elseClause
			n.elseClause.accept(this);
			
			assembler.add(new LlvmBranch(brBreak));
			
		} else {
			//Faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brBreak se retornar 0
			assembler.add(new LlvmBranch(cond, brTrue, brBreak));
			//Label brTrue
			assembler.add(new LlvmLabel(brTrue));
			
			//Desce para thenClause
			n.thenClause.accept(this);

			//System.out.format("thenClause: %s\n",n.thenClause);
			
			//Se pulou para label brTrue, agora ele pula para o brBreak, com o intuito de pular o brFalse
			assembler.add(new LlvmBranch(brBreak));
		}
		
		assembler.add(new LlvmLabel(brBreak));
		
		return null;
	}
	
	//While
	public LlvmValue visit(While n){
		
		//pega o registrador referente a condicao do while em cond
		LlvmValue cond = n.condition.accept(this);
		//System.out.format("cond: %s\n",cond);
		//System.out.format("n.condition: %s\n", n.condition);
		
		//Cria as labels referentes a cada branch
		
		//Cria label referente ao while.
		LlvmLabelValue brTrue = new LlvmLabelValue("whilelabeltrue"+counter_label);
		counter_label++;
		//System.out.format("label1: %s\n",brTrue);
		
		//Cria label referente ao break do while.
		LlvmLabelValue brBreak = new LlvmLabelValue("whilelabelbreak"+counter_label);
		counter_label++;
		//System.out.format("label2: %s\n",brBreak);
				
		//Faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brBreak se retornar 0
		//(ou seja, da break quando nao atende mais a condicao)
		
		assembler.add(new LlvmBranch(cond, brTrue, brBreak));
				
		//label brTrue
		assembler.add(new LlvmLabel(brTrue));
		
		//System.out.format("body: %s\n",n.body);
		
		//Desce para body do while
		n.body.accept(this);
		
		//Tivemos que colocar um accept para a condition aqui, pois estava entrando em loop infinito
		LlvmValue new_cond = n.condition.accept(this);
		
		//Depois de executar o codigo dentro do while, faz de novo o branch, com a condicao verificada novamente. 
		assembler.add(new LlvmBranch(new_cond, brTrue, brBreak));
		//label do break
		assembler.add(new LlvmLabel(brBreak));
			
		return null;
	}
	
	//Assign: pega-se o address da variavel e faz um store no valor do assign nesse endereco
	public LlvmValue visit(Assign n){
		
		System.out.format("assign********\n");
		
		LlvmValue rhs = n.exp.accept(this);
		LlvmRegister returns;
		//Nesta parte, para retornarmos o tipo certo, tivemos que converter todos os parametros do tipo
		//[ A x iB] para ponteiros. o Assembly reclamava quando tinha algum store ou algo do genero com tipos
		// diferentes
		if(rhs.type.toString().contains("x i")){
			//System.out.format("expressao de rhs envolve arrays. fazendo casting...\n");
			
			//Fazer bitcast
			if(rhs.type.toString().contains(" x i32")){
				returns = new LlvmRegister(LlvmPrimitiveType.I32PTR);
			}else if(rhs.type.toString().contains(" x i8")){
				returns = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
			//Esse else eh meio inutil, mas pelo fato de eu querer usar elseif, deixei ele aqui mesmo.
			}else{
				returns = new LlvmRegister(rhs.type);
			}
			
			assembler.add(new LlvmBitcast(returns, rhs, returns.type));
			assembler.add(new LlvmStore(returns, n.var.accept(this)));
		}else{
			//Caso o tipo ja esteja ok, soh damos store com o rhs mesmo.
			assembler.add(new LlvmStore(rhs, n.var.accept(this)));
		}
		return null;
	}
	
	//ArrayAssign
	public LlvmValue visit(ArrayAssign n){
		System.out.format("assign array:*******\n");
		
		//A ideia eh a mesma que um lookup, dando store na posicao que eu pegar
		LlvmValue array = n.var.accept(this);
		LlvmValue index = n.index.accept(this);
		LlvmValue value = n.value.accept(this);
		
		//System.out.format("array assign var,index,value: %s\n%s\n%s\n",array,index,value);
		
		//Cria registrador auxiliar que recebera o ponteiro para a posicao do vetor a ser atribuida
		LlvmRegister reg = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		
		//Calcula offset para o elemento desejado
		List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(index);
		
		//Carregando a array, agora meu reg_array_ptr vai ter a array em questao
		LlvmRegister reg_array_ptr = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		assembler.add(new LlvmLoad(reg_array_ptr, array));
		
		//Agora pedimos o ponteiro para o elemento que queremos da array
		assembler.add(new LlvmGetElementPointer(reg, reg_array_ptr, offsets));
		
		//e armazenamos o valor nesse endereco.
		assembler.add(new LlvmStore(value, reg));
		
		return null;
	}

	//And logico
	public LlvmValue visit(And n){
		
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);

		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		LlvmRegister aux1 = new LlvmRegister(LlvmPrimitiveType.I1);
		
		//Para termos certeza de que soh sera executado o codigo quando tivermos true e true, vamos usar o LlvmTimes
		//para multiplicar um pelo outro. Assim, quando ao menos um deles for false, teremos resposta zero
		
		//O tipo de v1 e v2 eh o mesmo, entao passamos o de v1
		assembler.add(new LlvmTimes(aux1, v1.type, v1, v2));
		
		LlvmIntegerLiteral aux2 = new LlvmIntegerLiteral(1);
		
		//Compara o resultado da multiplicacao com 1(pois todas as vezes que um for false, o resultado de mul vai ser zero).
		assembler.add(new LlvmIcmp(lhs,1,aux1.type,aux1,aux2));
		
		//Retorna o registrador contendo o resultado
		return lhs;
	}

	//LessThan
	public LlvmValue visit(LessThan n){
		//Accept em ambos os lados da expressao
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		//utilizando 0 para lessthan
		
		//System.out.format("less than: lhs e rhs: %s %s\n",n.lhs,n.rhs);
		
		//Comparamos dois elementos i32, e retornamos um bool.
		assembler.add(new LlvmIcmp(lhs,0,v1.type,v1,v2));
		return lhs;
	}
	
	//Equal
	public LlvmValue visit(Equal n){
		//Descendo nos dois lados
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		
		//Criando registrador bool para retornar
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		
		//utilizando 1 para equal
		assembler.add(new LlvmIcmp(lhs,1,v1.type,v1,v2));
		return lhs;
	}
	
	//Minus
	public LlvmValue visit(Minus n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmMinus(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;
	}
	
	//Times
	public LlvmValue visit(Times n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmTimes(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;  
	}
	
	//ArrayLookup
	public LlvmValue visit(ArrayLookup n){
		System.out.format("arraylookup*******\n");
		
		//Pego o array e o indice desejado
		LlvmValue array = n.array.accept(this);
		LlvmValue index = n.index.accept(this);
		
		//System.out.format("arraylookup array e index: %s \n%s\n",array,index);
		
		//Crio registrador que vai apontar para o elemento desejado
		LlvmRegister reg = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		//Calculo offset a partir do indice
		List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(index);
		/***************************************************/
		//Carregando a array, agora meu reg_array_ptr vai ter a array em questao
		LlvmRegister reg_array_ptr = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		assembler.add(new LlvmLoad(reg_array_ptr, array));
				
		//Agora pedimos o ponteiro para o elemento que queremos da array
		assembler.add(new LlvmGetElementPointer(reg, reg_array_ptr, offsets));
				
		/***********************************************/
		
		LlvmRegister reg_address_offset = new LlvmRegister(LlvmPrimitiveType.I32);
		
		//Carregamos o valor contido no endereco, e retornamos ele
		assembler.add(new LlvmLoad(reg_address_offset, reg));
		
		return reg_address_offset;
		
	}
	
	//ArrayLength
	//Nao consegui acessar o length da array.
	//Portanto, vamos ter que carregar a array e verificar
	public LlvmValue visit(ArrayLength n){
		
		int index = 0;
		char type_char;
		System.out.format("arraylength:******\n");

		//System.out.format("****n, n.array: %s \n%s\n",n,n.array);
		
		//Desce para o array, e pega o registrador que aponta para ela
		LlvmValue array = n.array.accept(this);
		
		//System.out.format("array e array.type: %s \n%s\n",array,array.type);
		
		StringBuilder type = new StringBuilder();
		type.append(array.type.toString());
		
		//System.out.format("type: %s\n",type);
		
		StringBuilder lengths = new StringBuilder();
		
		//Temos que verificar se a array foi alocada dinamicamente ou estaticamente, para poder
		//alterar a forma de pegar o length.
		if(array.type.toString().contains("* *")){
			//Dinamicamente... nesse caso alocamos o primeiro slot da array para colocar o tamanho
			//System.out.format("array declarada dinamicamente... carregando tamanho do primeiro endereco\n");
			
			//Carregando tamanho do inicio da array
			LinkedList<LlvmValue> offsets = new LinkedList<LlvmValue>();
			
			//Offsets do getelementptr
			offsets.add(new LlvmIntegerLiteral(0));
			
			//Registrador onde receberemos o pointer para o array (**)
			LlvmRegister length_ptr = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32PTR));

			//Pegando o endereco da array
			assembler.add(new LlvmGetElementPointer(length_ptr, array, offsets));
			
			//Registrador no qual vamos carregar a array (*)
			LlvmRegister array_reg = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
			
			//Array carregada
			assembler.add(new LlvmLoad(array_reg, length_ptr));
			
			//Registrador no qual vamos carregar o primeiro elemento da array
			LlvmRegister array_size = new LlvmRegister(LlvmPrimitiveType.I32);
			
			//Carrega o size do endereco inicial da array.
			assembler.add(new LlvmLoad(array_size, array_reg));
			
			return array_size;
		}
		else{
			//estaticamente. dessa forma conseguimos retirar o tamanho da array pelo tipo dela.
			//agora vamos parsear esse tamanho, em busca do tamanho total da string...
			
			while(true){
				type_char = type.charAt(index);
				if(type_char=='i'){
					//System.out.format("fim\n");
					break;
				}
				//System.out.format("char atual:%c\n",type.charAt(index));
				if(type_char=='x'){
					//System.out.format("x\n");
					lengths.append(" ");
				}
				
				//Achei um numero...
				if(type_char != 'x' && type_char != ' ' && type_char != '['){
					//System.out.format("numero: %c\n", type_char);
					lengths.append(type_char);
				}
				if(type_char == '['){
					//System.out.format("[\n");
				}
				if(type_char == ' '){
					//System.out.format("space\n");
				}
				index++;
			}
			
			//System.out.format("lengths: %s\n", lengths);
			//System.out.format("lengths.length: %s\n", lengths.length());
			index = 0;
			
			StringBuilder length = new StringBuilder();
			int total_length = 1;
			while(index < lengths.length()){
				type_char = lengths.charAt(index);
				if(type_char == '\0'){
					//System.out.format("fim\n");
					break;
				}
				if(type_char!=' '){
					length.append(type_char);
				}else{
					//System.out.format("space\n");
					//para cada nivel de array, multiplicamos pelo nivel anterior.
					//por exemplo, se for [10 x [20 x i32]] fica 10x20
					total_length = total_length * Integer.parseInt(length.toString());
					//System.out.format("total_length: %d\n",total_length);
					length = new StringBuilder();
				}
				index++;
			}
			
			LlvmIntegerLiteral length_final = new LlvmIntegerLiteral(total_length);
			
			return length_final;
		}
	}
	
	//Call
	public LlvmValue visit(Call n){
		
		System.out.format("call:*********\n");
		
		//System.out.format("call: %s\n",n);

		LlvmType type = (LlvmType) n.type.accept(this);
		
		LlvmRegister register = new LlvmRegister(type);
		
		//System.out.format("type:%s\n", n.type);
		
		//Lista dos tipos dos parametros da funcao sendo chamada
		List<LlvmType> types_list = new ArrayList<LlvmType>();
		
		//Lista dos parametros da funcao sendo chamada
		List<LlvmValue> parametros_func = new ArrayList<LlvmValue>();
		
		int i, j; 
		
		//primeiro adicionamos o this object a lista de parametros. (%class %this)
		LlvmValue thisObject = n.object.accept(this);
		
		types_list.add(thisObject.type);
		parametros_func.add(thisObject);
		
		//Depois os parametros de vdd
		if(n.actuals != null){
			
			//System.out.format("size actuals:%s\n", n.actuals.size());
			
			j = n.actuals.size();
			
			for (util.List<Exp> actuals = n.actuals; actuals != null; actuals = actuals.tail){
				
				LlvmValue aux = actuals.head.accept(this);
				
				//Tipo do parametro atual
				LlvmType aux2 =(LlvmType) aux.type;
				
				//System.out.format("actuals:%s\n", n.actuals.head);
				
				//System.out.format("actuals type:%s\n", n.actuals.head.type);
				
				//incrementando as duas listas
				types_list.add(aux2);
				parametros_func.add(aux);
			}
		
		}
		
		//System.out.format("@call n.method.s:%s\n", n.method.s);
		
		//Gerando o assembly do call da funcao, alterando o nome dela para "@__"+nome, pois esse eh o padrao dos metodos que estamos utilizando.
		assembler.add(new LlvmCall(register,type,types_list,"@__"+n.method.s,parametros_func));
		
		return register;
		
	}
	
	//True
	public LlvmValue visit(True n){
		//1 -> true
		return new LlvmBool(1);
	}
	
	//False
	public LlvmValue visit(False n){
		//0 -> false
		return new LlvmBool(0);
	}
	
	//IdentifierExp
	public LlvmValue visit(IdentifierExp n){
		System.out.format("identifierexp:********\n");
		
		//System.out.format("n.type: %s\n",n.type);
		//System.out.format("n.name: %s\n",n.name);
		//System.out.format("n.string: %s\n",n.toString());
		
		//Pega tipo do identificador
		LlvmPrimitiveType identifier_type = (LlvmPrimitiveType) n.type.accept(this);
		
		//Registrador que vai ser retornado
		LlvmRegister returns = new LlvmRegister(identifier_type);
		
		//Apontador vindo do identifier, que armazena a informacao que queremos.
		LlvmValue address = n.name.accept(this);
		//System.out.format("@identifierexp address: %s\n",address);
		
		//Ponteiro que passaremos para o load
		LlvmNamedValue needed_info_ptr = new LlvmNamedValue(address.toString(),address.type);
		
		//System.out.format("@identifierexp address infos: %s %s\n", address.toString(), address.type);
		
		if(address.type.toString().contains("* *")){
			//System.out.format("retornando ponteiro....\n");
			return needed_info_ptr;
		}
		
		//returns = load type * %where_from_load
		assembler.add(new LlvmLoad(returns, needed_info_ptr));
		
		return returns;
	}
	
	//This
	public LlvmValue visit(This n){
		
		System.out.format("This******** :)\n");
		
		//Pega o tipo do object
		LlvmType thisType = (LlvmType) n.type.accept(this);
		
		LlvmPointer thisType_ptr = new LlvmPointer(thisType);
		
		//System.out.format("thistype: %s\n",thisType_ptr);
		
		//Cria um registrador referente ao this
		LlvmRegister thisReg = new LlvmRegister("%this",thisType_ptr);
		
		return thisReg;
		
	}
	
	//NewArray
	public LlvmValue visit(NewArray n){
	
		System.out.format("newarray:********\n");
		
		//Pego o tamanho e o tipo
		LlvmValue tamanho = n.size.accept(this);
		LlvmType type = (LlvmType) n.type.accept(this);
		
		//System.out.format("tamanho: %s :)\n",tamanho);
		//System.out.format("tamanho.type: %s :)\n",tamanho.type);
		//System.out.format("@newarray type: %s\n",type);
		
		//Se o tamanho do array for passado por uma variável...
		if(tamanho.toString().contains("%")){
			
			//para conseguir armazenar o tamanho, em uma array mallocada, vamos
			//armazenar na primeira posicao dela o tamanho, e depois a array em si.
			
			//registrador no qual sera alocada a memoria necessaria
			LlvmRegister returns = new LlvmRegister(LlvmPrimitiveType.I32PTR);
			
			LlvmRegister final_size_reg = new LlvmRegister(LlvmPrimitiveType.I32);
			
			//soma um ao tamanho, referente ao espaco do tamanho no inicio da array
			assembler.add(new LlvmPlus(final_size_reg, LlvmPrimitiveType.I32, tamanho,new LlvmIntegerLiteral(1)));
			
			//System.out.format("@final size reg: %s\n",final_size_reg);
			
			//Alocando memoria necessaria para a array e para o tamanho.
			assembler.add(new LlvmMalloc(returns, LlvmPrimitiveType.I32, final_size_reg));
			
			//Inserindo tamanho no inicio da array
			LinkedList<LlvmValue> offsets = new LinkedList<LlvmValue>();
			
			//Offsets do getelementptr
			offsets.add(new LlvmIntegerLiteral(0));
			
			//Registrador onde receberemos o pointer para o endereco inicial
			LlvmRegister length_ptr = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));

			//Pegando o endereco para o elemento inicial (onde vai ficar o size)
			assembler.add(new LlvmGetElementPointer(length_ptr, returns, offsets));
			
			//Armazenando o size no endereco.
			assembler.add(new LlvmStore(tamanho, length_ptr));
			
			//Retorna registrador que contem o ponteiro para o endereco alocado.
			return returns;
			
			
		}else{ //Se o tamanho eh estatico
		
			//Como nos testes nao utilizamos array de outros tipos, fica assim por hora...
			LlvmType tipo_array = tamanho.type;
			
			//System.out.format("tamanho.type: %s :)\n",tipo_array);
			int tamanho_int = Integer.parseInt(tamanho.toString());
	
			//System.out.format("***Tamanho, tamanho_int, tipo, etc: %s %s %d %s %s:)\n",tamanho,tipo_array,tamanho_int,n.type,n.toString());
			
			// [10 x i32] *
			LlvmPointer tipo_ptr = new LlvmPointer(new LlvmArray(tamanho_int, tipo_array));
			
			//reg para [10 x i32] *
			LlvmRegister registrador = new LlvmRegister(tipo_ptr);
			
			//Aloca uma array [tamanho x type], associa ao um registrador do tipo [tamanho x type]*
			assembler.add(new LlvmAlloca(registrador, new LlvmArray(tamanho_int, tipo_array), new LinkedList<LlvmValue>()));
			
			return registrador;
		}
		
	}
	
	public LlvmValue visit(NewObject n){
		System.out.format("newobject:*******\n");
		
		//System.out.format("n.type:%s :)\n",n.type);
		//System.out.format("*****n:%s %s\n:)\n",n, n.className);
		
		LlvmType type = (LlvmType) (n.type.accept(this));
		
		//System.out.format("n.type | n.type.content: %s %s :)\n",n.type,type);
		
		LlvmPointer type_ptr = new LlvmPointer(type);
		
		//Cria registrador que tem tipo type *
		LlvmRegister returns = new LlvmRegister(type_ptr);
		
		//Aloca memoria para o objeto
		assembler.add(new LlvmMalloc(returns, type, type.toString()));
		
		return returns;
		
	}
	
	//Not
	public LlvmValue visit(Not n){
		
		//Ideia era um xor com 1, porque ele inverte o bit. 1 xor 1 = 0 e 1 xor 0 = 1
		//Para nao implementar xor usou-se o Icmp com equal e 0 (primeiro termo fixo), 
		//pois 0 == 0 é true (1) e 0 == 1 é false, ou seja, 0.
		LlvmValue v1 = n.exp.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
        assembler.add(new LlvmIcmp(lhs,1,v1.type,v1,new LlvmBool(0)));
        return lhs;
        
	}
	
	//Identifier
	public LlvmValue visit(Identifier n){
		System.out.format("identifier:******\n");

		//System.out.format("********identifier: %s |\n",n);
		
		//TODO: descobrir como pegar o tipo do identifier
		//descoberto! usando symtab
		
		LlvmNamedValue identifier = null;
		int index;
		int i;
		LlvmValue identified;
		boolean aux;
		
		//Para encontrar o tipo do identifier, vamos buscar na symtab, e ver o que ele realmente eh.
		
		//System.out.format("bp\n");
		
		if(methodEnv != null){
			aux = methodEnv.formals_name.contains(n.s);
			//System.out.format("formals: %s\n",aux);
			if(aux){
				index = methodEnv.formals_name.indexOf(n.s);
				identified = methodEnv.formals_value.get(index);
				//System.out.format("identified: %s\n",identified);
				//Encontrei valor nos formals. então a variavel eh um formals.
				identifier = new LlvmNamedValue("%"+n.s+"_address",new LlvmPointer(identified.type));
			}else{
				//Se nao achou, vamos procurar no locals
				aux = methodEnv.locals_name.contains(n.s);
				//System.out.format("locals: %s\n",aux);
				if(aux){
					index = methodEnv.locals_name.indexOf(n.s);
					identified = methodEnv.locals_value.get(index);
					//System.out.format("identified: %s\n",identified);
					//Encontrei valor nos locals. então a variavel eh um formals.
					identifier = new LlvmNamedValue("%"+n.s+"_address",new LlvmPointer(identified.type));
				}else{
					//Se nao achou no locals, vamos procurar no classenv
					//dai devemos retornar um getelementptr
					
					StringBuilder var_name = new StringBuilder();
					
					var_name.append("%");
					var_name.append(n.s);
					var_name.append("_address");
					
					index = classEnv.varList.size();
					for(i=0;i<index;i++){
						LlvmValue var_atual = classEnv.varList.get(i);
						//System.out.format("varList: %s\n",var_atual);
						if(var_atual.toString().contains(var_name.toString())){
							
							LlvmType vartype = var_atual.type;
							//System.out.format("@identifier: vartype: %s\n",vartype);
							//agora que encontramos o identifier, vamos usar o getelementptr para pegar os valores dele.
							
							//registrador no qual sera colocado o ponteiro para o elemento em questao. 
							//utilizamos LlvmPointer porque esse regisrtador vai apontar para o endereco que aponta para o vartype
							LlvmRegister returns = new LlvmRegister(new LlvmPointer(vartype));
							
							//variavel onde colocaremos os offsets
							List<LlvmValue> offsets = new LinkedList<LlvmValue>();
							
							//peguei da mainclass new LlvmIntegerLiteral(0)
							//primeiro indice: ponteiro para vetor de ponteiros (queremos sempre a base!)
							LlvmValue offset_1 = new LlvmIntegerLiteral(0);
							
							//segundo indice: indice do elemento do ponteiro apontado pelo indice 1.
							//como acabamos de dar match nele, colocamos i.
							LlvmValue offset_2 = new LlvmIntegerLiteral(i);
							
							//primeiro indice
							offsets.add(offset_1);
							
							//segundo indice
							offsets.add(offset_2);
							
							//do slide 21/47 da parte 1: %tmp0 = getelementptr %class.Matematica * %this, i32 0, i32 0
							
							//LlvmNamedValue classUsed = new LlvmNamedValue("%class."+classEnv.nameClass, vartype);
							
							LlvmClassInfo classType = new LlvmClassInfo(classEnv.nameClass);
							
							LlvmPointer classType_ptr = new LlvmPointer(classType);
							
							//Criando source (do GetElementPtr)
							LlvmNamedValue source = new LlvmNamedValue("%this",classType_ptr);
							
							assembler.add(new LlvmGetElementPointer(returns, source, offsets));
							
							//System.out.format("returns:%s \n %s\n %s\n",returns,returns.name,returns.type);
							
							return returns;
							
						}
					}
				
				}
			}
		}else{
			//se nao achou no locals, vamos procurar no classenv
			//dai devemos retornar um getelementptr
			//ToDo: suportar getelementptr
			
			StringBuilder var_name = new StringBuilder();
			
			var_name.append("%");
			var_name.append(n.s);
			var_name.append("_address");
			
			index = classEnv.varList.size();
			for(i=0;i<index;i++){
				LlvmValue var_atual = classEnv.varList.get(i);
				//System.out.format("varList: %s\n",var_atual);
				if(var_atual.toString().contains(var_name.toString())){
					//System.out.format("le migueh! encontrei o endereco da variavel22.\n");
					LlvmType vartype = var_atual.type;
					//System.out.format("@identifier: vartype: %s\n",vartype);
					//agora que encontramos o identifier, vamos usar o getelementptr para pegar os valores dele.
					
					//registrador no qual sera colocado o ponteiro para o elemento em questao. 
					//utilizamos LlvmPointer porque esse regisrtador vai apontar para o endereco que aponta para o vartype
					LlvmRegister returns = new LlvmRegister(new LlvmPointer(vartype));
					
					//variavel onde colocaremos os offsets
					List<LlvmValue> offsets = new LinkedList<LlvmValue>();
					
					//peguei da mainclass new LlvmIntegerLiteral(0)
					//primeiro indice: ponteiro para vetor de ponteiros (queremos sempre a base!)
					LlvmValue offset_1 = new LlvmIntegerLiteral(0);
					
					//segundo indice: indice do elemento do ponteiro apontado pelo indice 1.
					//como acabamos de dar match nele, colocamos i.
					LlvmValue offset_2 = new LlvmIntegerLiteral(i);
					
					//primeiro indice
					offsets.add(offset_1);
					
					//segundo indice
					offsets.add(offset_2);
					
					//do slide 21/47 da parte 1: %tmp0 = getelementptr %class.Matematica * %this, i32 0, i32 0
					
					//LlvmNamedValue classUsed = new LlvmNamedValue("%class."+classEnv.nameClass, vartype);
					
					LlvmClassInfo classType = new LlvmClassInfo(classEnv.nameClass);
					
					LlvmPointer classType_ptr = new LlvmPointer(classType);
					
					//Criando source (do GetElementPtr)
					LlvmNamedValue source = new LlvmNamedValue("%this",classType_ptr);
					
					assembler.add(new LlvmGetElementPointer(returns, source, offsets));
					
					//System.out.format("returns:%s \n %s\n %s\n",returns,returns.name,returns.type);
					
					return returns;
				}
			}
			
		}
		
		return identifier;
		
	}
}


/**********************************************************************************/
/* === Tabela de Símbolos ==== 
 * 
 * 
 */
/**********************************************************************************/

class SymTab extends VisitorAdapter{
    public Map<String, ClassNode> classes;
    public Map<String, MethodNode> methods;
    //sera utilizado nas visitas
    private ClassNode classEnv;    //aponta para a classe em uso
    private MethodNode methodEnv;   //aponta para metodo em uso
    //cnstrct
    public SymTab(){
    	classes = new HashMap<String, ClassNode>();
    	methods = new HashMap<String, MethodNode>();
    }
    
    public LlvmValue FillTabSymbol(Program n){
    	System.out.format("Comecando o preenchimento da symtab...\n");
    	n.accept(this);
    	return null;
}
public LlvmValue visit(Program n){
	n.mainClass.accept(this);

	for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
		c.head.accept(this);

	return null;
}

public LlvmValue visit(MainClass n){
	//System.out.format("n.className: %s\n",n.className.s);
	
	//System.out.format("Comecando o preenchimento da symtab MainClass...\n");
	classes.put(n.className.s, new ClassNode(n.className.s, null, null));
	//System.out.format("Comecando o preenchimento da symtab MainClass22...\n");
	return null;
}

public LlvmValue visit(ClassDeclSimple n){
	
	System.out.format("@SymTabClassDeclSimple...\n");
	
	List<LlvmType> typeList = new LinkedList<LlvmType>();
	// Constroi TypeList com os tipos das variáveis da Classe (vai formar a Struct da classe)
	
	List<LlvmValue> varList = new LinkedList<LlvmValue>();
	// Constroi VarList com as Variáveis da Classe

	int i,j;
	
	if(n.varList!=null){
		//int varListSize = n.varList.size();
		
		for (util.List<VarDecl> vars = n.varList; vars != null; vars = vars.tail){
			LlvmValue variable = vars.head.accept(this);
			
			//System.out.format("variable: %s\n",variable);
			
			varList.add(variable);
			typeList.add((LlvmType) vars.head.type.accept(this));
		}
		
	}
	
	//System.out.format("@SymTab escrevendo no classes: \n%s \n%s \n%s\n",n.name.s, new LlvmStructure(typeList), varList);
	//cria classnode referente a classe atual. Sera utilizado na hora de guardar os metodos
	ClassNode classe_atual = new ClassNode(n.name.s, new LlvmStructure(typeList), varList);
	
	//adiciona a classe ao mapa de classes
	classes.put(n.name.s, classe_atual);
	
	//vai ser utilizado na parte de metodos
	classEnv = classe_atual;
	
	if(n.methodList != null) {
		j = n.methodList.size();
		//itera todos os metodos da classe
		
		//Retirado do exemplo da main...
		// Percorre n.methodList visitando cada método
		for (util.List<MethodDecl> method = n.methodList; method != null; method = method.tail){
			System.out.format("@SymTab class - method: %s ", method);
			method.head.accept(this);
		}
		
	}
	
	return null;
}

//provavelmente nao vamos tratar
	public LlvmValue visit(ClassDeclExtends n){
		System.out.format("@SymTabClassDeclExtends...\n");
		return null;}
	public LlvmValue visit(VarDecl n){
		System.out.format("@SymTabVarDecl...\n");
		
		//var type
		LlvmType type = (LlvmType)n.type.accept(this);
		
		StringBuilder name = new StringBuilder();
		
		name.append("%");
		name.append(n.name.s);
		name.append("_address");
		
		LlvmNamedValue var = new LlvmNamedValue(name.toString(),type);
		
		return var;
		//return null;
		
	}
	
	public LlvmValue visit(Formal n){
		System.out.format("@SymTabFormal...\n");
		
		//var type
		LlvmType type = (LlvmType)n.type.accept(this);
				
		StringBuilder name = new StringBuilder();
				
		name.append("%");
		name.append(n.name.s);
		name.append("_address");
				
		LlvmNamedValue var = new LlvmNamedValue(name.toString(),type);
				
		return var;
		//return null;
	}
	
	public LlvmValue visit(MethodDecl n){
		System.out.format("@SymTabMethodDecl...\n");
		
		//System.out.format("n.name.s: %s\n", n.name.s);
		
		int i,j;
		
		//parametros da SymTab
		List<String> formals_name = new LinkedList<String>();;
		List<LlvmValue> formals_value = new LinkedList<LlvmValue>();;
		List<String> locals_name = new LinkedList<String>();;
		List<LlvmValue> locals_value = new LinkedList<LlvmValue>();;
		
		//preenchendo a lista de formals do metodo
		if(n.formals != null) {
			//System.out.format("formals: %s\n",n.formals.head);
			j = n.formals.size();
			//System.out.format("formals size: %d\n", j);
			
			for (util.List<Formal> formals = n.formals; formals != null; formals = formals.tail){
				//System.out.format("@SymTab method - formals: %s ", formals);
				
				LlvmValue param = formals.head.accept(this);
				
				//System.out.format("@SymTab formals head: %s \n", n.formals.head);
				//System.out.format("@SymTab formals head value: %s \n", param);
				
				//Grava nas listas os nomes+values dos formals do metodo
				formals_name.add(formals.head.name.toString());
				formals_value.add(param);
			}
			
		}
		
		//Preenchendo a lista de locals do metodo
		if(n.locals!=null){
			//System.out.format("locals size: %s\n",n.locals.size());
			j = n.locals.size();
			
			for (util.List<VarDecl> locals = n.locals; locals != null; locals = locals.tail){
				//System.out.format("@SymTab method - locals: %s ", locals);
				
				LlvmValue param2 = locals.head.accept(this);
				
				//System.out.format("@SymTab locals head: %s \n", n.locals.head);
				//System.out.format("@SymTab locals head value: %s \n", param2);
				
				//Grava nas listas os nomes+values dos formals do metodo
				locals_name.add(locals.head.name.toString());
				locals_value.add(param2);
			}
			
		}
		
		//System.out.format("escrevendo no methods: \n%s \n%s \n%s \n%s \n%s\n",n.name.s,formals_name,formals_value,locals_name,locals_value);
		
		MethodNode method_atual = new MethodNode(n.name.s,formals_name,formals_value,locals_name,locals_value);
		
		//adiciona metodo ao methods
		methods.put(n.name.s,method_atual);
		
		//classEnv.methods.put(n.name.s, method_atual);
		
		return null;
		}
	
	public LlvmValue visit(IdentifierType n){
		System.out.format("@SymTabIdentifierType...\n");
		//System.out.format("identifiertype :)\n");
		
		//%class.name
		StringBuilder name = new StringBuilder();

		name.append(n.name);
		
		//System.out.format("name: %s\n",name.toString());
		
		//cria classType
		LlvmClassInfo classType = new LlvmClassInfo(name.toString());

		return classType;
		//return null;
		
		}
	public LlvmValue visit(IntArrayType n){
		System.out.format("@SymTabIntArrayType...\n");
		
		return LlvmPrimitiveType.I32PTR;
		//return null;
		}
	public LlvmValue visit(BooleanType n){
		System.out.format("@SymTabBooleanType...\n");
		
		return LlvmPrimitiveType.I1;
		//return null;
	}
	public LlvmValue visit(IntegerType n){
		System.out.format("@SymTabIntegerType...\n");
		
		return LlvmPrimitiveType.I32;
		
		//return null;
		}
}

class ClassNode extends LlvmType {
	public String nameClass;
	public LlvmStructure classType;
	public List<LlvmValue> varList;
	ClassNode (String nameClass, LlvmStructure classType, List<LlvmValue> varList){
		this.nameClass = nameClass;
		this.classType = classType;
		this.varList = varList;
	}
}

//nesta classe, armazenaremos todas as informacoes dos metodos.
//nome do metodo, variaveis locais e parametros.
class MethodNode extends LlvmType{
	
	public String name;
	
	//nome e value do formals
	
	public List<String> formals_name;
	public List<LlvmValue> formals_value;
	
	//nome e value do locals
	
	public List<String> locals_name;
	public List<LlvmValue> locals_value;
	
	MethodNode(String name, List<String> formals_name, List<LlvmValue> formals_value, List<String> locals_name, List<LlvmValue> locals_value){
		this.name = name;
		this.formals_name = formals_name;
		this.formals_value = formals_value;
		this.locals_name = locals_name;
		this.locals_value = locals_value;
	}
}




