export function getLoggedInUser() {
    const storedUser = localStorage.getItem("yatayatUser");
  
    if (!storedUser) return null;
  
    try {
      return JSON.parse(storedUser);
    } catch {
      return null;
    }
  }
  
  export function getFirstName() {
    const user = getLoggedInUser();
    return user?.fullName?.split(" ")[0] || "User";
  }
  
  export function getFullName() {
    const user = getLoggedInUser();
    return user?.fullName || "User";
  }
  
  export function getUserEmail() {
    const user = getLoggedInUser();
    return user?.email || "";
  }
  
  export function getUserPhone() {
    const user = getLoggedInUser();
    return user?.phone || "";
  }
  
  export function getUserRole() {
    const user = getLoggedInUser();
    return user?.role || "";
  }