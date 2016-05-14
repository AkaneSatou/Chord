/**
 * 
 */
package peersim.chord;

import peersim.core.*;
import peersim.config.Configuration;
import java.math.*;

/**
 * @author Andrea
 * 
 */
public class CreateNw implements Control {

	private int pid = 0;

	private static final String PAR_IDLENGTH = "idLength";

	private static final String PAR_PROT = "protocol";

	private static final String PAR_SUCCSIZE = "succListSize";

	int idLength = 0;

	int successorLsize = 0;

	int fingSize = 0;
	//campo x debug
	boolean verbose = false;

	/**
	 * 
	 */
	public CreateNw(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		idLength = Configuration.getInt(prefix + "." + PAR_IDLENGTH); 
		successorLsize = Configuration.getInt(prefix + "." + PAR_SUCCSIZE); 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see peersim.core.Control#execute()
	 */

	public boolean execute() {
		//recorre toda la red uno por uno
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = (Node) Network.get(i); //obtiene el nodo de la iteración i
			ChordProtocol cp = (ChordProtocol) node.getProtocol(pid); //obtiene el pid para chord protocol
			cp.m = idLength; //al chord protocol le asigna el tamaño del idLenght
			cp.succLSize = successorLsize; //define el tamaño de la lista de sucesores
			cp.varSuccList = 0;
			cp.chordId = new BigInteger(idLength, CommonState.r);
			cp.fingerTable = new Node[idLength]; //array del largo del idLenght para la fingerTable
			cp.successorList = new Node[successorLsize];//array del tamaño de la lista de sucesores
		}
		NodeComparator nc = new NodeComparator(pid); //comparador de nodos de acuerdo al pid
		Network.sort(nc);//los ordena los nodos de acuerdo al comparador de nodos
		createFingerTable(); //crea la fingerTable
		return false;
	}

	public Node findId(BigInteger id, int nodeOne, int nodeTwo) { //encontrar id entre dos nodos?
		if (nodeOne >= (nodeTwo - 1)) //si esto se cumple, entonces hay un solo nodo en la red
			return Network.get(nodeOne);//retorna el único nodo existente
		int middle = (nodeOne + nodeTwo) / 2; //busca la mitad de los nodos
		if (((middle) >= Network.size() - 1)) //si la mitad es más grande que la red, no existe
			System.out.print("ERROR: Middle is bigger than Network.size");
		if (((middle) <= 0))//si la mitad es cero, entonces hay un unico nodo
			return Network.get(0); //se obtiene el primero y el único
		try { //si no es ninguno de los casos anteriores
			BigInteger newId = ((ChordProtocol) ((Node) Network.get(middle)) //toma la mitad
					.getProtocol(pid)).chordId; //obtiene el id en chord
			BigInteger lowId; //antiguo id?
			if (middle > 0) //si está en la parte superior, obtiene el id en chord
				lowId = ((ChordProtocol) ((Node) Network.get(middle - 1))
						.getProtocol(pid)).chordId;//obtiene el id de la mitad menos 1
			else //si se encuentra en la parte inferior
				lowId = newId;
			BigInteger highId = ((ChordProtocol) ((Node) Network
					.get(middle + 1)).getProtocol(pid)).chordId;//sino obtiene el id de mitad +1
			if (id.compareTo(newId) == 0 || ((id.compareTo(newId) == 1) && (id.compareTo(highId) == -1))) {
				return Network.get(middle); //si el id corresponde la la mitad, entonces es la mitad 
			}
			if ((id.compareTo(newId) == -1) && (id.compareTo(lowId) == 1)) {
				if (middle > 0)
					return Network.get(middle - 1);
				else
					return Network.get(0);
			}
			if (id.compareTo(newId) == -1) {
				return findId(id, nodeOne, middle);
			} else if (id.compareTo(newId) == 1) {
				return findId(id, middle, nodeTwo);
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void createFingerTable() {
		//toma los valores del primero y el ultimo
		BigInteger idFirst = ((ChordProtocol) Network.get(0).getProtocol(pid)).chordId;
		BigInteger idLast = ((ChordProtocol) Network.get(Network.size() - 1)
				.getProtocol(pid)).chordId;
		for (int i = 0; i < Network.size(); i++) {
			Node node = (Node) Network.get(i);
			ChordProtocol cp = (ChordProtocol) node.getProtocol(pid);
			for (int a = 0; a < successorLsize; a++) {
				
				if (a + i < (Network.size() - 1))
					cp.successorList[a] = Network.get(a + i + 1);
					
				else
					cp.successorList[a] = Network.get(a);
			}
			if (i > 0)
				cp.predecessor = (Node) Network.get(i - 1);
			else
				cp.predecessor = (Node) Network.get(Network.size() - 1);
			int j = 0;
			for (j = 0; j < idLength; j++) {
				BigInteger base;
				if (j == 0)
					base = BigInteger.ONE;
				else {
					base = BigInteger.valueOf(2);
					for (int exp = 1; exp < j; exp++) {
						base = base.multiply(BigInteger.valueOf(2));
					}
				}
				BigInteger pot = cp.chordId.add(base);
				
				if (pot.compareTo(idLast) == 1) {
					pot = (pot.mod(idLast));
					if (pot.compareTo(cp.chordId) != -1) {
						break;
					}
					if (pot.compareTo(idFirst) == -1) {
						cp.fingerTable[j] = Network.get(Network.size() - 1);
						continue;
					}
				}
				cp.fingerTable[j] = findId(pot, 0, Network.size() - 1);
			}
		}
	}
}
