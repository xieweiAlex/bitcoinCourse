import java.util.ArrayList;

public class TxHandler {
    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        ArrayList<UTXO> utxoList = new ArrayList<>();
        double inputSum = 0.0;
        double outputSum = 0.0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);

            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!pool.contains(utxo)) {
               return false;
            }

            Transaction.Output output = pool.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            if (utxoList.contains(utxo)) {
                return false;
            }


            if (output.value < 0) {
                return false;
            }

            utxoList.add(utxo);

            inputSum += output.value;
        }

        for (Transaction.Output output: tx.getOutputs()) {
           outputSum += output.value;
        }

        return inputSum >= outputSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for (Transaction tx: possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);
            }

            for(int i = 0; i < tx.numOutputs(); i++) {
                UTXO utxo = new UTXO(tx.getHash(), i);
                pool.addUTXO(utxo, tx.getOutput(i));
            }

            for(int i = 0; i < tx.numInputs(); i++) {
                Transaction.Input input = tx.getInput(i);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                pool.removeUTXO(utxo);
            }
        }



        Transaction[] validTxsArray = new Transaction[validTxs.size()];
        validTxsArray = validTxs.toArray(validTxsArray);
        return validTxsArray;
    }

}
