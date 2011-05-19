package org.omscentral.modules.analysis.esp;


public class ElNino {

/*
**  Definition of categories:
**    LA_NINA - Water Year NINO3.4 SSTs < -0.5 C
**    EL_NINO - Water Year NINO3.4 SSTs > 0.5 C
**    NEUTRAL - Water Year NINO3.4 SSTs < 0.5 and > -0.5 C
**    NEG_PDO - PDO < -0.5
**    POS_PDO - PDO > 0.5
**    NEU_PDO - PDO Neutral
*/

   public static int UNKNOWN = 0;
   public static int LA_NINA = 1;
   public static int NEUTRAL = 2;
   public static int EL_NINO = 3;
   public static int NEG_PDO = 4;
   public static int POS_PDO = 5;
   public static int NEU_PDO = 6;

/*
**  Years start at 1872
*/
   private static int[] enso_year_codes= {
      LA_NINA,     // 1872
      LA_NINA,     // 1873
      LA_NINA,     // 1874
      LA_NINA,     // 1875
      LA_NINA,     // 1876
      EL_NINO,     // 1877
      EL_NINO,     // 1878
      NEUTRAL,     // 1879
      LA_NINA,     // 1880
      NEUTRAL,     // 1881
      NEUTRAL,     // 1882
      NEUTRAL,     // 1883
      NEUTRAL,     // 1884
      EL_NINO,     // 1885
      NEUTRAL,     // 1886
      LA_NINA,     // 1887
      EL_NINO,     // 1888
      EL_NINO,     // 1889
      LA_NINA,     // 1890
      NEUTRAL,     // 1891
      NEUTRAL,     // 1892
      LA_NINA,     // 1893
      LA_NINA,     // 1894
      NEUTRAL,     // 1895
      NEUTRAL,     // 1896
      EL_NINO,     // 1897
      NEUTRAL,     // 1898
      NEUTRAL,     // 1899
      EL_NINO,     // 1900
      NEUTRAL,     // 1901
      NEUTRAL,     // 1902
      EL_NINO,     // 1903
      NEUTRAL,     // 1904
      EL_NINO,     // 1905
      NEUTRAL,     // 1906
      NEUTRAL,     // 1907
      NEUTRAL,     // 1908
      LA_NINA,     // 1909
      LA_NINA,     // 1910
      LA_NINA,     // 1911
      EL_NINO,     // 1912
      NEUTRAL,     // 1913
      EL_NINO,     // 1914
      EL_NINO,     // 1915
      LA_NINA,     // 1916
      LA_NINA,     // 1917
      NEUTRAL,     // 1918
      EL_NINO,     // 1919
      NEUTRAL,     // 1920
      NEUTRAL,     // 1921
      NEUTRAL,     // 1922
      NEUTRAL,     // 1923
      NEUTRAL,     // 1924
      LA_NINA,     // 1925
      EL_NINO,     // 1926
      NEUTRAL,     // 1927
      NEUTRAL,     // 1928
      NEUTRAL,     // 1929
      EL_NINO,     // 1930
      EL_NINO,     // 1931
      NEUTRAL,     // 1932
      LA_NINA,     // 1933
      LA_NINA,     // 1934
      NEUTRAL,     // 1935
      NEUTRAL,     // 1936
      NEUTRAL,     // 1937
      LA_NINA,     // 1938
      NEUTRAL,     // 1939
      EL_NINO,     // 1940
      EL_NINO,     // 1941
      NEUTRAL,     // 1942
      LA_NINA,     // 1943
      NEUTRAL,     // 1944
      NEUTRAL,     // 1945
      NEUTRAL,     // 1946
      NEUTRAL,     // 1947
      NEUTRAL,     // 1948
      NEUTRAL,     // 1949
      LA_NINA,     // 1950
      NEUTRAL,     // 1951
      NEUTRAL,     // 1952
      NEUTRAL,     // 1953
      NEUTRAL,     // 1954
      NEUTRAL,     // 1955
      LA_NINA,     // 1956
      NEUTRAL,     // 1957
      EL_NINO,     // 1958
      NEUTRAL,     // 1959
      NEUTRAL,     // 1960
      NEUTRAL,     // 1961
      NEUTRAL,     // 1962
      NEUTRAL,     // 1963
      NEUTRAL,     // 1964
      NEUTRAL,     // 1965
      EL_NINO,     // 1966
      NEUTRAL,     // 1967
      NEUTRAL,     // 1968
      EL_NINO,     // 1969
      NEUTRAL,     // 1970
      LA_NINA,     // 1971
      NEUTRAL,     // 1972
      NEUTRAL,     // 1973
      LA_NINA,     // 1974
      LA_NINA,     // 1975
      LA_NINA,     // 1976
      EL_NINO,     // 1977
      NEUTRAL,     // 1978
      NEUTRAL,     // 1979
      NEUTRAL,     // 1980
      NEUTRAL,     // 1981
      EL_NINO,     // 1982
      EL_NINO,     // 1983
      NEUTRAL,     // 1984
      LA_NINA,     // 1985
      NEUTRAL,     // 1986
      EL_NINO,     // 1987
      NEUTRAL,     // 1988
      LA_NINA,     // 1989
      NEUTRAL,     // 1990
      EL_NINO,     // 1991
      EL_NINO,     // 1992
      EL_NINO,     // 1993
      EL_NINO,     // 1994
      EL_NINO,     // 1995
      NEUTRAL,     // 1996
      EL_NINO,     // 1997
      EL_NINO      // 1998
   };

/*
**  Years start at 1901
*/
   private static int[] pdo_year_codes= {
      NEU_PDO,     // 1901
      POS_PDO,     // 1902
      NEU_PDO,     // 1903
      NEU_PDO,     // 1904
      POS_PDO,     // 1905
      NEU_PDO,     // 1906
      NEU_PDO,     // 1907
      NEU_PDO,     // 1908
      NEU_PDO,     // 1909
      NEU_PDO,     // 1910
      NEU_PDO,     // 1911
      NEU_PDO,     // 1912
      POS_PDO,     // 1913
      NEU_PDO,     // 1914
      NEU_PDO,     // 1915
      NEU_PDO,     // 1916
      NEU_PDO,     // 1917
      NEG_PDO,     // 1918
      NEU_PDO,     // 1919
      NEG_PDO,     // 1920
      NEU_PDO,     // 1921
      NEU_PDO,     // 1922
      NEU_PDO,     // 1923
      NEU_PDO,     // 1924
      NEU_PDO,     // 1925
      POS_PDO,     // 1926
      POS_PDO,     // 1927
      NEU_PDO,     // 1928
      NEU_PDO,     // 1929
      NEU_PDO,     // 1930
      POS_PDO,     // 1931
      NEU_PDO,     // 1932
      NEG_PDO,     // 1933
      POS_PDO,     // 1934
      POS_PDO,     // 1935
      POS_PDO,     // 1936
      POS_PDO,     // 1937
      NEU_PDO,     // 1938
      NEU_PDO,     // 1939
      POS_PDO,     // 1940
      POS_PDO,     // 1941
      POS_PDO,     // 1942
      NEU_PDO,     // 1943
      NEU_PDO,     // 1944
      NEU_PDO,     // 1945
      NEG_PDO,     // 1946
      NEU_PDO,     // 1947
      NEU_PDO,     // 1948
      NEG_PDO,     // 1949
      NEG_PDO,     // 1950
      NEG_PDO,     // 1951
      NEG_PDO,     // 1952
      NEU_PDO,     // 1953
      NEU_PDO,     // 1954
      NEG_PDO,     // 1955
      NEG_PDO,     // 1956
      NEU_PDO,     // 1957
      POS_PDO,     // 1958
      NEU_PDO,     // 1959
      NEU_PDO,     // 1960
      NEU_PDO,     // 1961
      NEG_PDO,     // 1962
      NEG_PDO,     // 1963
      NEG_PDO,     // 1964
      NEU_PDO,     // 1965
      NEU_PDO,     // 1966
      NEG_PDO,     // 1967
      NEU_PDO,     // 1968
      NEU_PDO,     // 1969
      NEU_PDO,     // 1970
      NEG_PDO,     // 1971
      NEG_PDO,     // 1972
      NEU_PDO,     // 1973
      NEG_PDO,     // 1974
      NEG_PDO,     // 1975
      NEG_PDO,     // 1976
      POS_PDO,     // 1977
      NEU_PDO,     // 1978
      NEU_PDO,     // 1979
      POS_PDO,     // 1980
      POS_PDO,     // 1981
      NEU_PDO,     // 1982
      POS_PDO,     // 1983
      POS_PDO,     // 1984
      POS_PDO,     // 1985
      POS_PDO,     // 1986
      POS_PDO,     // 1987
      POS_PDO,     // 1988
      NEU_PDO,     // 1989
      NEU_PDO,     // 1990
      NEG_PDO,     // 1991
      POS_PDO,     // 1992
      POS_PDO,     // 1993
      POS_PDO,     // 1994
      NEU_PDO,     // 1995
      POS_PDO,     // 1996
      POS_PDO,     // 1997
      POS_PDO,     // 1998
      NEG_PDO      // 1999
   };

   public static boolean lookUp(int cat, EnsembleListLabel ell) {
       return lookUp(cat, ell.getTraceYear());
   }
   
   public static boolean lookUp (int cat, int year) {
       if ((cat == LA_NINA) || (cat == NEUTRAL) || (cat == EL_NINO)) {
           if ((year < 1872) || (year > 1998)) return false;
           if (enso_year_codes[(year - 1872)] == cat) return true;
       } else if ((cat == NEG_PDO) || (cat == POS_PDO) || (cat == NEU_PDO)) {
           if ((year < 1901) || (year > 1999)) return false;
           if (pdo_year_codes[(year - 1901)] == cat) return true;
       }
       return false;
   }
}
