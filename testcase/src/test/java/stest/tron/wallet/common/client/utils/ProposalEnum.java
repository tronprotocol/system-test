package stest.tron.wallet.common.client.utils;

public enum ProposalEnum {

  GetAllowNewResourceModel("getAllowNewResourceModel"),
  GetUnfreezeDelayDays("getUnfreezeDelayDays"),
  GetAllowDynamicEnergy("getAllowDynamicEnergy"),
  GetDynamicEnergyThreshold("getDynamicEnergyThreshold"),
  GetDynamicEnergyIncreaseFactor("getDynamicEnergyIncreaseFactor"),
  GetDynamicEnergyMaxFactor("getDynamicEnergyMaxFactor"),
  GetAllowOptimizedReturnValueOfChainId("getAllowOptimizedReturnValueOfChainId"),
  GetMemoFee("getMemoFee"),
  GetEnergyFee("getEnergyFee"),
  getMaxFeeLimit("getMaxFeeLimit"),
  getMaxCpuTimeOfOneTx("getMaxCpuTimeOfOneTx"),
  getAllowCancelUnfreezeV2("getAllowCancelUnfreezeV2"),
  getMaxDelegateLockPeriod("getMaxDelegateLockPeriod");

  private String proposalName;

  ProposalEnum(String proposalName) {
    this.proposalName = proposalName;
  }

  public String getProposalName() {
    return proposalName;
  }

}
