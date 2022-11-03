package stest.tron.wallet.common.client.utils;

public enum ProposalEnum {

  GetAllowNewResourceModel("getAllowNewResourceModel"),
  GetUnfreezeDelayDays("getUnfreezeDelayDays"),

  GetMemoFee("getMemoFee");

  private String proposalName;

  ProposalEnum(String proposalName) {
    this.proposalName = proposalName;
  }

  public String getProposalName() {
    return proposalName;
  }

}
