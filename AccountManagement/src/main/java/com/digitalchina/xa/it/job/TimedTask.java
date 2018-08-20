package com.digitalchina.xa.it.job;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import com.digitalchina.xa.it.model.WalletTransactionDomain;
import com.digitalchina.xa.it.service.WalletTransactionService;

import scala.util.Random;

@Component
public class TimedTask {
	@Autowired
	private WalletTransactionService walletTransactionService;
	private static String[] ip = {"http://10.7.10.124:8545","http://10.7.10.125:8545","http://10.0.5.217:8545","http://10.0.5.218:8545","http://10.0.5.219:8545"};

	@Transactional
	@Scheduled(cron="10,40 * * * * ?")
	public void updateVoteTopic(){
		Web3j web3j = Web3j.build(new HttpService(ip[new Random().nextInt(5)]));
		List<String> transactionHashAll = walletTransactionService.selectTransactionHashUnconfirm();
		if(transactionHashAll == null) {
			return;
		}
		try {
			for(int i = 0; i < transactionHashAll.size(); i++) {
				String transactionHash = transactionHashAll.get(i);
				TransactionReceipt tr = web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get().getResult();
				if(tr == null) {
					return;
				}
				if(!tr.getBlockHash().contains("00000000")) {
					BigInteger gasUsed = tr.getGasUsed();
					BigInteger blockNumber = tr.getBlockNumber();
					WalletTransactionDomain wtd = new WalletTransactionDomain();
					wtd.setTransactionHash(transactionHash);
					wtd.setGas(Double.valueOf(gasUsed.toString()));
					wtd.setConfirmBlock(Integer.valueOf(blockNumber.toString()));
					wtd.setStatus(1);
					walletTransactionService.updateByTransactionHash(wtd);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}
