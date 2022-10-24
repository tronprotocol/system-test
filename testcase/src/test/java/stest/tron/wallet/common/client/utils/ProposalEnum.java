package stest.tron.wallet.common.client.utils;

public enum ProposalEnum {

  GetAllowNewResourceModel("getAllowNewResourceModel"),
  GetUnfreezeDelayDays("getUnfreezeDelayDays");

  private String proposalName;

  ProposalEnum(String proposalName) {
    this.proposalName = proposalName;
  }

  public String getProposalName() {
    return proposalName;
  }

}
