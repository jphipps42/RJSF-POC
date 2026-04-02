const express = require('express');
const cors = require('cors');
require('dotenv').config();

const formConfigRoutes = require('./routes/formConfigurations');
const formSubmissionRoutes = require('./routes/formSubmissions');
const awardRoutes = require('./routes/awards');
const personnelRoutes = require('./routes/personnel');
const linkedFilesRoutes = require('./routes/linkedFiles');
const schemaVersionRoutes = require('./routes/schemaVersions');

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());

// Routes
app.use('/api/form-configurations', formConfigRoutes);
app.use('/api/form-submissions', formSubmissionRoutes);
app.use('/api/awards', awardRoutes);
app.use('/api/personnel', personnelRoutes);
app.use('/api/linked-files', linkedFilesRoutes);
app.use('/api/schema-versions', schemaVersionRoutes);

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
