import java.util.ArrayList;
import java.util.LinkedList;


public class PseudoTree {
	OrNode root;
	GraphicalModel model;
	ArrayList<OrNode> orNodes;
	LinkedList<int[]> cachedSamples;
	
	public PseudoTree(GraphicalModel model) {
		this.model = model;
	}
	
	public OrNode generatePseudoTree() {
		ArrayList<Integer> softOrder = model.computeSoftOrder();
		ArrayList<ArrayList<Factor>> clusters = model.generateSoftClusters(softOrder);
		ArrayList<Variable> order = model.orderIntToOrderVar(softOrder);
		
		// indice of empty clusters in order
		ArrayList<Integer> emptyCluster = new ArrayList<>();
		for (int i = 0; i < clusters.size(); i++) {
			ArrayList<Factor> cluster = clusters.get(i);
			if(0 == cluster.size()) {
				emptyCluster.add(i);
			}
		}
		
		orNodes = new ArrayList<>(order.size());
		for (Variable variable : order) {
			orNodes.add(new OrNode(variable));
		}
		
		root = orNodes.get(orNodes.size() - 1);
		for (int i = 0; i < order.size(); i++) {
			Variable thisVariable = order.get(i);
			ArrayList<Factor> thisCluster = clusters.get(i);
			OrNode thisOrNode = orNodes.get(i);
			thisOrNode.cluster = thisCluster;
			
			ArrayList<Variable> tmpVars = new ArrayList<>();
			for (Factor factor : thisCluster) {
				for (Variable var : factor.variables) {
					if(var != thisVariable) {
						if(!tmpVars.contains(var)) {
							tmpVars.add(var);
						}
					}
				}
			}
			Factor newFactor = new Factor(tmpVars);
			
			for (int j = i + 1; j < order.size(); j++) {
				Variable thatVariable = order.get(j);
				OrNode thatOrNode = orNodes.get(j);
				ArrayList<Factor> thatCluster = clusters.get(j);
				
				if(newFactor.inScope(thatVariable)) {
					thatOrNode.addChildren(thisOrNode);
					thatCluster.add(newFactor);
					break;
				}
			}
		}
		
		// replace the previously empty cluster with non-sumout factors
		int prev = 0;
		for (Integer emptyClusterIndex : emptyCluster) {
			Variable respectVar = model.getVariable(softOrder.get(emptyClusterIndex));
			OrNode respectOrNode = orNodes.get(softOrder.get(emptyClusterIndex));
			respectOrNode.cluster.clear();
			//ArrayList<Factor> newCluster = new ArrayList<>();
			for (int i = prev; i <= emptyClusterIndex - 1; i++) {
				ArrayList<Factor> clusterPrevInOrder = clusters.get(i);
				ArrayList<Factor> newFactors = new ArrayList<>();
				Variable toBeElminated = model.getVariable(softOrder.get(i));
				boolean mentionEmpty = false;
				for (Factor factor : clusterPrevInOrder) {
					if(factor.inScope(respectVar)) {
						mentionEmpty = true;
						newFactors.add(factor);
					}
				}
				if(!mentionEmpty) {
					continue;
				}
				
				Factor newFactor = Eliminator.Product(newFactors);
				newFactor = Eliminator.SumOut(newFactor, toBeElminated);
				respectOrNode.cluster.add(newFactor);
			}
			//clusters.set(emptyClusterIndex, newCluster);
			prev = emptyClusterIndex;
		}
		
		return root;
	}
	
	public int treeHeight() {
		return treeHeight(root);
	}
	
	public int treeHeight(TreeNode node) {
		if(null == node) {
			return 0;
		}
		
		int height = 0;
		for (TreeNode child : node.children) {
			int tmp = treeHeight(child);
			if(tmp > height) {
				height = tmp;
			}
		}
		
		return height;
	}
	
	public void splitOrNodes() {
		splitOrNodes(root);
	}
	
	public void splitOrNodes(OrNode orNode) {
		if(null == orNode) {
			return;
		}
		
		if (orNode.alreadySplitted) {
			return;
		}
		
		// backup of Or children
		ArrayList<TreeNode> orChildren = orNode.children;
		ArrayList<Variable> varChildren = new ArrayList<>();
		for (TreeNode treeNode : orChildren) {
			varChildren.add(((OrNode)treeNode).nodeVariable);
		}
		// split to And children
		orNode.children = new ArrayList<>(orNode.nodeVariable.domainSize());
		for (int i = 0; i < orNode.nodeVariable.domainSize(); i++) {
			orChildren.add(new AndNode(i));
		}
		
		for (TreeNode andNode : orNode.children) {
			splitAndNodes(varChildren, (AndNode)andNode);
		}
	}
	
	public void splitAndNodes(ArrayList<Variable> vars, AndNode andNode) {
		for (Variable var : vars) {
			andNode.addChildren(new OrNode(var));
		}
		
		for (TreeNode orNode : andNode.children) {
			splitOrNodes((OrNode)orNode);
		}
		
		if (vars.size() == 0) {
			andNode.V = 1;
		}
	}
	
	public void initializeArcs() {
		for (OrNode orNode : orNodes) {
			// find the child of order
			LinkedList<OrNode> below = new LinkedList<>();
			// only need to traverse one And child, because all the And child are same
			// in terms of structure
			findOrNodesBelow(orNode, below);
		}
	}
	
	
	
	public void findOrNodesBelow(OrNode orNode, LinkedList<OrNode> below) {
		// find the child of order
		//LinkedList<Variable> below = new LinkedList<>();
		// only need to traverse one And child, because all the And child are same
		// in terms of structure
		if(0 == orNode.children.size()) {
			return;
		}
		
		AndNode andNode = (AndNode) orNode.children.get(0);
		for (TreeNode orNode2 : andNode.children) {
			below.add((OrNode) orNode2);
			findOrNodesBelow((OrNode) orNode2, below);
		}
	}
}
