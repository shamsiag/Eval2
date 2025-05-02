<template>
    <div class="login-page">
      <div class="left-section">
        <h1 class="login-title">Connexion</h1>
  
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="input-group">
            <label for="email">Email</label>
            <input
              type="email"
              id="email"
              v-model="email"
              placeholder="Entrez votre email"
              required
            />
          </div>
  
          <div class="input-group">
            <label for="password">Mot de passe</label>
            <input
              type="password"
              id="password"
              v-model="password"
              placeholder="Entrez votre mot de passe"
              required
            />
          </div>
  
          <div class="form-footer">
            <button type="submit" class="login-button">Se connecter</button>
            <p class="signup-link">
              Pas de compte ?
              <router-link to="/signup">Inscrivez-vous ici</router-link>
            </p>
          </div>
        </form>
  
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      </div>
  
      <div class="right-section">
        <img src="@/assets/item/login-illustration.jpg" alt="Illustration" class="login-image" />
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: "LoginPage",
    data() {
      return {
        email: "",
        password: "",
        errorMessage: "",
      };
    },
    methods: {
        async handleLogin() {
  this.errorMessage = "";

  try {
    const response = await fetch("http://localhost:8080/api/v1/auth/tokens", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        userName: "GardenAdmin",
        password: "GardenAdmin",
        parameters: {
          clientId: 11,
          roleId: 102,
          organizationId: 0,
          language: "en_US"
        }
      })
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || "Erreur d'authentification.");
    }

    const data = await response.json();

    localStorage.setItem("authToken", data.token);
    localStorage.setItem("refreshToken", data.refresh_token);
    localStorage.setItem("userId", data.userId);
    localStorage.setItem("language", data.language);
    localStorage.setItem("menuTreeId", data.menuTreeId);

    this.$router.push("/shop");
    
  } catch (error) {
    console.error("Erreur lors de la connexion :", error);
    this.errorMessage = error.message || "Erreur de connexion. Veuillez réessayer plus tard.";
  }
}

    },
  };
  </script>
  
  <style scoped>
  @font-face {
    font-family: 'PP Formula';
    src: url('@/assets/fonts/PPFormula-NarrowRegular.otf') format('opentype');
    font-weight: medium;
  }
  
  @font-face {
    font-family: 'DAWBE';
    src: url('@/assets/fonts/dawbe.otf') format('opentype');
    font-weight: bold;
  }
  
  .login-page {
    display: flex;
    height: 100vh;
    background-color: #0e100e;
    color: #f7f7f7;
    font-family: 'PP Formula', sans-serif;
  }
  
  .left-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 60px;
    text-align: left;
  }
  
  .right-section {
    flex: 1;
    background-color: #1e1e1e;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .login-image {
    max-width: 90%;
    height: auto;
    border-radius: 10px;
  }
  
  .login-title {
    font-size: 56px;
    font-family: 'DAWBE';
    margin-bottom: 40px;
  }
  
  .login-form {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  
  .input-group {
    display: flex;
    flex-direction: column;
  }
  
  .input-group label {
    margin-bottom: 5px;
    font-weight: bold;
  }
  
  .input-group input {
    padding: 10px;
    border-radius: 5px;
    border: none;
    font-size: 1em;
  }
  
  .form-footer {
    margin-top: 20px;
  }
  
  .login-button {
    padding: 10px 20px;
    background-color: #5aaad8;
    border: none;
    border-radius: 5px;
    color: white;
    font-family: 'PP Formula', sans-serif;
    cursor: pointer;
    transition: background-color 0.3s ease;
  }
  
  .login-button:hover {
    background-color: #3e8cbf;
  }
  
  .signup-link {
    margin-top: 15px;
    font-size: 0.9em;
  }
  
  .signup-link a {
    color: #5aaad8;
    text-decoration: underline;
  }
  
  .error-message {
    margin-top: 15px;
    color: red;
  }
  </style>
  