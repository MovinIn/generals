package botting.generals;

public enum Accounts {
  GAPPED("PPo7gaXEb"),RANDOM_1("CBfiJ5oBm");

  private final String userid;

  Accounts(String userid){
    this.userid=userid;
  }

  public String as(){
    return userid;
  }
}
