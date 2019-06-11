package advExample;

public class AdvExamples {
	
	/**
	 * Static (build dependency): this->ret, i->ret, j->ret
	 * Static (output inference): ret
	 * Static (potential inputs): this, i, j (because they may contribute to output and they are params)
	 * @param i
	 * @param j
	 * @return
	 */
	public int testCaller1(int i, int j) {
		//Dynamic: i is output, j is output, because they are loaded by callee
		int ret = testCallee1(i, j);
		//Dynamic: ret is the input, because ret is the output of the callee
		return ret;
	}
	
	/**
	 * Static (build dependency): i->i+j, j->i+j
	 * Static (output inference): i+J
	 * Static (potential inputs): i, j (because they may contribute to output and they are params)
	 * @param i
	 * @param j
	 * @return
	 */
	public int testCallee1(int i, int j) {
		return i + j;
	}
}
